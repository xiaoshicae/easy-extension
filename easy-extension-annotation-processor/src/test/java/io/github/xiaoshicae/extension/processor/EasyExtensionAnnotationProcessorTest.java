package io.github.xiaoshicae.extension.processor;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EasyExtensionAnnotationProcessorTest {

    @Test
    public void testSupportedSourceVersionFollowsTheCompiler() {
        // A hard-coded RELEASE_xx constant makes the processor unusable on older JDKs.
        assertEquals(SourceVersion.latestSupported(), new EasyExtensionAnnotationProcessor().getSupportedSourceVersion());
    }

    @Test
    public void testGeneratesMetadataForAnnotatedInterface(@TempDir Path out) throws Exception {
        String source = """
                package demo;

                import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

                /** Computes freight for an order. */
                @ExtensionPoint(version = 2)
                public interface FreightExt {
                    String calc();
                }
                """;

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        compile(out, diagnostics, "demo.FreightExt", source);

        assertTrue(errors(diagnostics).isEmpty(), "unexpected compile errors: " + errors(diagnostics));

        Path metadata = out.resolve("META-INF/easy-extension/metadata.json");
        assertTrue(Files.exists(metadata), "metadata.json should be generated next to the class files");

        String json = Files.readString(metadata);
        assertTrue(json.contains("\"qualifiedName\": \"demo.FreightExt\""), json);
        assertTrue(json.contains("\"annotationType\": \"ExtensionPoint\""), json);
        assertTrue(json.contains("Computes freight for an order."), "javadoc should be extracted: " + json);
        assertTrue(json.contains("interface FreightExt"), "source code should be extracted: " + json);
        assertTrue(json.contains("\"version\": 2"), "annotation attributes should be extracted: " + json);
    }

    @Test
    public void testNoMetadataWhenNothingIsAnnotated(@TempDir Path out) throws Exception {
        String source = """
                package demo;

                public class Plain {
                }
                """;

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        compile(out, diagnostics, "demo.Plain", source);

        assertTrue(errors(diagnostics).isEmpty(), "unexpected compile errors: " + errors(diagnostics));
        assertFalse(Files.exists(out.resolve("META-INF/easy-extension/metadata.json")));
    }

    private static void compile(Path out, DiagnosticCollector<JavaFileObject> diagnostics,
                                String className, String source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "tests must run on a JDK, not a JRE");

        // The test source only needs the annotation types from core on its compile classpath.
        String coreLocation = Path.of(ExtensionPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        List<String> options = List.of("-d", out.toString(), "-classpath", coreLocation);

        JavaFileObject file = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };

        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, List.of(file));
        task.setProcessors(List.of(new EasyExtensionAnnotationProcessor()));
        assertTrue(task.call(), "compilation failed: " + diagnostics.getDiagnostics());
    }

    private static List<Diagnostic<? extends JavaFileObject>> errors(DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
    }
}
