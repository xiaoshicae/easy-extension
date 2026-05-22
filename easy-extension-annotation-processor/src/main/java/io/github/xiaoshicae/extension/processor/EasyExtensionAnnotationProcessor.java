package io.github.xiaoshicae.extension.processor;

import com.sun.source.util.Trees;
import com.sun.source.util.TreePath;
import com.sun.source.tree.CompilationUnitTree;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编译期注解处理器，自动提取 Javadoc 和源码生成 metadata.json。
 * <p>
 * 用户在编译时引入此处理器后，每个模块编译产物中会包含
 * {@code META-INF/easy-extension/metadata.json}，运行时 admin 模块
 * 读取该文件即可获得完整的源码和注释，无需依赖 sources.jar。
 * </p>
 */
@SupportedAnnotationTypes({
        "io.github.xiaoshicae.extension.core.annotation.ExtensionPoint",
        "io.github.xiaoshicae.extension.core.annotation.Ability",
        "io.github.xiaoshicae.extension.core.annotation.Business",
        "io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class EasyExtensionAnnotationProcessor extends AbstractProcessor {

    static final String OUTPUT_PATH = "META-INF/easy-extension/metadata.json";
    static final String METADATA_VERSION = "1.0";

    private static final int MAX_WALK_DEPTH = 12;
    private static final String[] SRC_ROOTS = {"src/main/java", "src/main/kotlin", "src"};
    private static final Set<String> PROJECT_ROOT_MARKERS = Set.of(
            "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");

    private final List<ClassMetadata> collectedMetadata = new ArrayList<>();
    private boolean written = false;
    private Trees trees;
    private boolean treesUnavailableWarned = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (trees == null && !treesUnavailableWarned) {
            try {
                trees = Trees.instance(processingEnv);
            } catch (Exception e) {
                // Trees API is only available under javac (com.sun.source.*). Ecj, some incremental
                // Gradle builds, or non-javac compilers will miss it. Promote to WARNING so users
                // know that Admin UI will show empty source code for their @ExtensionPoint classes.
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                        "[easy-extension] Trees API not available (compiler: "
                                + System.getProperty("java.vendor") + "). "
                                + "Source code extraction will be skipped; the Admin UI will display "
                                + "empty source for extension points. This is harmless at runtime. "
                                + "If source display matters, build with javac (Maven default) or "
                                + "publish sources.jar as a fallback. Cause: " + e.getMessage());
                treesUnavailableWarned = true;
            }
        }

        if (roundEnv.processingOver()) {
            if (!written) {
                writeMetadataFile();
                written = true;
            }
            return false;
        }

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement typeElement) {
                    processAnnotatedType(typeElement, annotation);
                }
            }
        }

        return false;
    }

    private void processAnnotatedType(TypeElement typeElement, TypeElement annotation) {
        String className = typeElement.getSimpleName().toString();
        String qualifiedName = typeElement.getQualifiedName().toString();
        String annotationType = annotation.getSimpleName().toString();

        String javadoc = extractJavadoc(typeElement);
        String sourceCode = extractSourceCode(typeElement);
        Map<String, Object> attributes = extractAnnotationAttributes(typeElement, annotation);

        collectedMetadata.add(new ClassMetadata(
                className, qualifiedName, annotationType,
                sourceCode, javadoc, attributes
        ));

        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "[easy-extension] Processed: " + qualifiedName);
    }

    private String extractJavadoc(TypeElement element) {
        String docComment = processingEnv.getElementUtils().getDocComment(element);
        return docComment != null ? docComment.strip() : "";
    }

    private String extractSourceCode(TypeElement typeElement) {
        // Step 1: Trees API (javac only — full original source with comments & formatting)
        if (trees != null) {
            try {
                TreePath path = trees.getPath(typeElement);
                if (path != null) {
                    CompilationUnitTree cu = path.getCompilationUnit();
                    return cu.getSourceFile().getCharContent(true).toString();
                }
            } catch (Exception e) {
                // fall through
            }
        }

        String qualifiedName = typeElement.getQualifiedName().toString();
        String relativePath = qualifiedName.replace('.', '/') + ".java";

        // Step 2: Walk up from CWD (bounded; stops at project root marker)
        String content = walkUpForSource(Path.of("").toAbsolutePath(), relativePath);
        if (content != null) return content;

        // Step 3: Scan CWD's first-level subdirectories — typical multi-module case
        content = scanSubdirsForSource(Path.of("").toAbsolutePath(), relativePath);
        if (content != null) return content;

        // Step 4: Walk up from CLASS_OUTPUT (e.g., target/classes, build/classes/java/main)
        Path classOutput = resolveClassOutputDir();
        if (classOutput != null) {
            content = walkUpForSource(classOutput, relativePath);
            if (content != null) return content;
        }

        // Step 5: Source not found — runtime will attempt file system fallback
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "[easy-extension] Source file not found for " + qualifiedName
                        + " — runtime will attempt file system lookup");
        return "";
    }

    private String walkUpForSource(Path start, String relativePath) {
        Path dir = start;
        for (int depth = 0; dir != null && depth < MAX_WALK_DEPTH; depth++) {
            String content = tryReadSource(dir, relativePath);
            if (content != null) return content;
            // Stop walking past a project root to avoid leaking into unrelated parent directories.
            if (hasProjectRootMarker(dir)) break;
            dir = dir.getParent();
        }
        return null;
    }

    private String scanSubdirsForSource(Path baseDir, String relativePath) {
        try (var entries = Files.newDirectoryStream(baseDir)) {
            for (Path entry : entries) {
                if (Files.isDirectory(entry)) {
                    String content = tryReadSource(entry, relativePath);
                    if (content != null) return content;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private boolean hasProjectRootMarker(Path dir) {
        for (String marker : PROJECT_ROOT_MARKERS) {
            if (Files.exists(dir.resolve(marker))) return true;
        }
        return false;
    }

    private String tryReadSource(Path baseDir, String relativePath) {
        for (String srcRoot : SRC_ROOTS) {
            Path srcFile = baseDir.resolve(srcRoot).resolve(relativePath);
            if (Files.exists(srcFile)) {
                try {
                    return Files.readString(srcFile);
                } catch (Exception e) {
                    // skip
                }
            }
        }
        return null;
    }

    /**
     * Derive the CLASS_OUTPUT directory by asking Filer for the URI of where our
     * own output resource would live, then trimming the {@link #OUTPUT_PATH} suffix.
     * Avoids creating/deleting a probe resource.
     */
    private Path resolveClassOutputDir() {
        try {
            FileObject probe = processingEnv.getFiler().getResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_PATH);
            Path metadataPath = Path.of(probe.toUri());
            // OUTPUT_PATH = "META-INF/easy-extension/metadata.json" — strip 3 segments
            Path p = metadataPath.getParent();
            for (int i = 0; i < 2 && p != null; i++) p = p.getParent();
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> extractAnnotationAttributes(TypeElement typeElement, TypeElement annotationType) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().asElement().equals(annotationType)) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> values =
                        processingEnv.getElementUtils().getElementValuesWithDefaults(mirror);

                for (var entry : values.entrySet()) {
                    String key = entry.getKey().getSimpleName().toString();
                    Object value = convertAnnotationValue(entry.getValue());
                    result.put(key, value);
                }
                break;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertAnnotationValue(AnnotationValue av) {
        Object value = av.getValue();
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> convertAnnotationValue((AnnotationValue) item))
                    .toList();
        }
        return value;
    }

    private void writeMetadataFile() {
        Map<String, OldEntry> oldEntries = readOldEntries();
        int mergedSources = 0;
        int emptyAfterMerge = 0;

        // Step 1: For current entries with empty sourceCode, reuse sourceCode from old file
        // (handles IDEA incremental builds where Trees API misses sources).
        for (int i = 0; i < collectedMetadata.size(); i++) {
            ClassMetadata m = collectedMetadata.get(i);
            if (m.sourceCode().isEmpty()) {
                OldEntry old = oldEntries.get(m.qualifiedName());
                if (old != null && !old.sourceCode().isEmpty()) {
                    collectedMetadata.set(i, new ClassMetadata(
                            m.className(), m.qualifiedName(), m.annotationType(),
                            old.sourceCode(), m.javadoc(), m.annotationAttributes()));
                    mergedSources++;
                } else {
                    emptyAfterMerge++;
                }
            }
        }

        // Step 2: Preserve entries from the old file that aren't being re-processed in this build
        // (incremental builds may only recompile a subset of @ExtensionPoint classes; without this,
        // those untouched entries would be silently dropped).
        Set<String> currentNames = new HashSet<>();
        for (ClassMetadata m : collectedMetadata) currentNames.add(m.qualifiedName());
        List<String> preservedRaw = new ArrayList<>();
        for (OldEntry old : oldEntries.values()) {
            if (!currentNames.contains(old.qualifiedName())) {
                preservedRaw.add(old.rawJson());
            }
        }

        int totalEntries = collectedMetadata.size() + preservedRaw.size();
        if (totalEntries == 0) {
            // Nothing to write and nothing to preserve — leave any existing file alone.
            return;
        }

        if (mergedSources > 0 || !preservedRaw.isEmpty() || emptyAfterMerge > 0) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[easy-extension] metadata.json merge: current=" + collectedMetadata.size()
                            + ", mergedFromOld=" + mergedSources
                            + ", preservedFromOld=" + preservedRaw.size()
                            + ", emptySources=" + emptyAfterMerge);
        }

        String json = buildJson(collectedMetadata, preservedRaw);

        try {
            FileObject resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_PATH);
            try (Writer writer = resource.openWriter()) {
                writer.write(json);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[easy-extension] Generated " + OUTPUT_PATH + " (" + totalEntries + " entries)");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "[easy-extension] Failed to write " + OUTPUT_PATH + ": " + e.getMessage());
        }
    }

    private String buildJson(List<ClassMetadata> current, List<String> preservedRaw) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": ").append(JsonWriter.quote(METADATA_VERSION)).append(",\n");
        sb.append("  \"generatedAt\": ").append(JsonWriter.quote(Instant.now().toString())).append(",\n");
        sb.append("  \"classes\": [\n");
        int total = current.size() + preservedRaw.size();
        int idx = 0;
        for (ClassMetadata m : current) {
            JsonWriter.writeClassMetadata(sb, m, "    ");
            idx++;
            if (idx < total) sb.append(",");
            sb.append("\n");
        }
        for (String raw : preservedRaw) {
            sb.append("    ").append(raw);
            idx++;
            if (idx < total) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private Map<String, OldEntry> readOldEntries() {
        Map<String, OldEntry> result = new LinkedHashMap<>();
        try {
            FileObject existing = processingEnv.getFiler().getResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_PATH);
            String content = existing.getCharContent(true).toString();
            int classesStart = content.indexOf("\"classes\"");
            if (classesStart < 0) return result;
            int arrayStart = content.indexOf('[', classesStart);
            if (arrayStart < 0) return result;
            int pos = arrayStart + 1;
            while (pos < content.length()) {
                int objStart = content.indexOf('{', pos);
                if (objStart < 0) break;
                int objEnd = findMatchingBrace(content, objStart);
                if (objEnd < 0) break;
                String obj = content.substring(objStart, objEnd + 1);
                String qn = extractJsonString(obj, "qualifiedName");
                if (qn != null && !qn.isEmpty()) {
                    String sc = extractJsonString(obj, "sourceCode");
                    result.put(qn, new OldEntry(qn, sc != null ? sc : "", obj));
                }
                pos = objEnd + 1;
            }
        } catch (Exception e) {
            // No existing metadata.json or can't read it — that's fine
        }
        return result;
    }

    private static int findMatchingBrace(String json, int startPos) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = startPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static String extractJsonString(String obj, String key) {
        String search = "\"" + key + "\"";
        int keyPos = obj.indexOf(search);
        if (keyPos < 0) return null;
        int colonPos = obj.indexOf(':', keyPos + search.length());
        if (colonPos < 0) return null;
        int valueStart = obj.indexOf('"', colonPos + 1);
        if (valueStart < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = valueStart + 1; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (escape) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    private record OldEntry(String qualifiedName, String sourceCode, String rawJson) {
    }
}
