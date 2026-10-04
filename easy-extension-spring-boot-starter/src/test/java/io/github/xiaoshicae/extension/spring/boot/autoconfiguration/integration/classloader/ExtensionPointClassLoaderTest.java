package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.classloader;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.DefaultResourceLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Like Spring Boot devtools (and other launchers): the starter is loaded by the base class loader, the classes of the
 * application, extension points included, by a child class loader that the base one does not see.
 */
public class ExtensionPointClassLoaderTest {

    @Test
    public void testExtensionPointIsFoundByTypeBeforeAnyFactoryBeanIsInitialized() throws Exception {
        Path dir = Files.createTempDirectory("easy-extension-application-classes");
        Path source = dir.resolve("restartable/Greeter.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package restartable;\n"
                + "@io.github.xiaoshicae.extension.core.annotation.ExtensionPoint\n"
                + "public interface Greeter { String greet(); }\n");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "needs a JDK");
        String coreClasses = Path.of(ExtensionPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        assertEquals(0, compiler.run(null, null, null, "-d", dir.toString(), "-cp", coreClasses, source.toString()));

        try (URLClassLoader applicationLoader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, ExtensionPointClassLoaderTest.class.getClassLoader())) {
            DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
            registry.setBeanClassLoader(applicationLoader);
            ExtensionPointScanner scanner = new ExtensionPointScanner(registry);
            scanner.setResourceLoader(new DefaultResourceLoader(applicationLoader));
            scanner.scan("restartable");

            Class<?> greeter = applicationLoader.loadClass("restartable.Greeter");
            // what @ConditionalOnBean(Greeter.class) asks: by type, without initializing factory beans
            String[] names = registry.getBeanNamesForType(greeter, true, false);

            assertEquals(1, names.length, "the bean for the extension point is known by its type");
        }
    }
}
