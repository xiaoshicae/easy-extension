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
            if (!collectedMetadata.isEmpty() && !written) {
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
        // Step 1: Try Trees API (javac only — full original source with comments & formatting)
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

        // Step 2: Try reading from filesystem
        // 2a: Walk up from CWD + check subdirs
        try {
            String relativePath = typeElement.getQualifiedName().toString().replace('.', '/') + ".java";
            Path dir = Path.of("").toAbsolutePath();
            while (dir != null) {
                String found = tryReadSource(dir, relativePath);
                if (found != null) return found;
                try (var entries = Files.newDirectoryStream(dir)) {
                    for (Path entry : entries) {
                        if (Files.isDirectory(entry)) {
                            found = tryReadSource(entry, relativePath);
                            if (found != null) return found;
                        }
                    }
                } catch (Exception e) { /* skip */ }
                dir = dir.getParent();
            }
        } catch (Exception e) {
            // fall through
        }

        // 2b: Infer module dir from CLASS_OUTPUT (more reliable in IDEA)
        try {
            String relativePath = typeElement.getQualifiedName().toString().replace('.', '/') + ".java";
            FileObject tmp = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", "_ee_tmp");
            Path classOutput = Path.of(tmp.toUri()).getParent();
            tmp.delete();
            Path dir = classOutput;
            while (dir != null) {
                String found = tryReadSource(dir, relativePath);
                if (found != null) return found;
                dir = dir.getParent();
            }
        } catch (Exception e) {
            // fall through
        }

        // Step 3: Source file not found — return empty so runtime can try file system
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "[easy-extension] Source file not found for " + typeElement.getQualifiedName()
                        + " — runtime will attempt file system lookup");
        return "";
    }

    private String tryReadSource(Path baseDir, String relativePath) {
        for (String srcRoot : new String[]{"src/main/java", "src/main/kotlin", "src"}) {
            Path srcFile = baseDir.resolve(srcRoot).resolve(relativePath);
            if (Files.exists(srcFile)) {
                try {
                    return Files.readString(srcFile);
                } catch (Exception e) { /* skip */ }
            }
        }
        return null;
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
        Map<String, String> oldSourceCodes = readOldSourceCodes();
        int merged = 0;
        int emptyCount = 0;

        for (ClassMetadata m : collectedMetadata) {
            if (m.sourceCode().isEmpty()) {
                emptyCount++;
                String old = oldSourceCodes.get(m.qualifiedName());
                if (old != null && !old.isEmpty()) {
                    int idx = collectedMetadata.indexOf(m);
                    collectedMetadata.set(idx, new ClassMetadata(
                            m.className(), m.qualifiedName(), m.annotationType(),
                            old, m.javadoc(), m.annotationAttributes()));
                    merged++;
                }
            }
        }

        // If ALL sourceCode is empty and no old metadata to merge from,
        // skip writing entirely to preserve any existing valid metadata.json
        if (emptyCount == collectedMetadata.size() && merged == 0 && oldSourceCodes.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "[easy-extension] Skipped writing metadata.json — no source files found "
                            + "(" + emptyCount + " entries), preserving existing file");
            return;
        }

        if (merged > 0) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[easy-extension] Preserved sourceCode for " + merged + " of " + emptyCount + " empty entries");
        }

        String json = JsonWriter.toJson(METADATA_VERSION, Instant.now().toString(), collectedMetadata);

        try {
            FileObject resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_PATH);
            try (Writer writer = resource.openWriter()) {
                writer.write(json);
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[easy-extension] Generated " + OUTPUT_PATH + " with " + collectedMetadata.size() + " entries");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "[easy-extension] Failed to write " + OUTPUT_PATH + ": " + e.getMessage());
        }
    }

    private Map<String, String> readOldSourceCodes() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            FileObject existing = processingEnv.getFiler().getResource(
                    StandardLocation.CLASS_OUTPUT, "", OUTPUT_PATH);
            String content = existing.getCharContent(true).toString();
            // Simple JSON parse to extract qualifiedName -> sourceCode pairs
            int classesStart = content.indexOf("\"classes\"");
            if (classesStart > 0) {
                int arrayStart = content.indexOf('[', classesStart);
                if (arrayStart > 0) {
                    int pos = arrayStart + 1;
                    while (pos < content.length()) {
                        int objStart = content.indexOf('{', pos);
                        if (objStart < 0) break;
                        int objEnd = findMatchingBrace(content, objStart);
                        if (objEnd < 0) break;
                        String obj = content.substring(objStart, objEnd + 1);
                        String qn = extractJsonString(obj, "qualifiedName");
                        String sc = extractJsonString(obj, "sourceCode");
                        if (qn != null && sc != null && !sc.isEmpty()) {
                            result.put(qn, sc);
                        }
                        pos = objEnd + 1;
                    }
                }
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
                    case '"' -> sb.append('"'); case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n'); case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    default -> sb.append(c);
                }
                escape = false; continue;
            }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }
}
