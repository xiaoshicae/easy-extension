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

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (trees == null) {
            try {
                trees = Trees.instance(processingEnv);
            } catch (Exception e) {
                // Trees API not available, source code extraction will fall back to empty
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "[easy-extension] Trees API not available, source code will not be extracted");
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
        if (trees == null) {
            return "";
        }
        try {
            TreePath path = trees.getPath(typeElement);
            if (path == null) return "";

            CompilationUnitTree cu = path.getCompilationUnit();
            CharSequence source = cu.getSourceFile().getCharContent(true);
            return source.toString();
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "[easy-extension] Could not read source for " + typeElement.getQualifiedName()
                            + ": " + e.getMessage());
            return "";
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
}
