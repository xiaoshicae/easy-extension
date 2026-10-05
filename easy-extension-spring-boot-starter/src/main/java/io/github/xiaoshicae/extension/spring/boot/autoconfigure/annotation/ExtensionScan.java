package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.ExtensionScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds packages to scan for extension points ({@code @ExtensionPoint} interfaces), abilities, businesses and default
 * implementations. Optional: the auto-configuration package of a Spring Boot application is always scanned; use this
 * for classes outside of it (e.g. another module), or in a context without auto-configuration.
 * The package of the annotated class is always included.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ExtensionScannerRegistrar.class)
public @interface ExtensionScan {

    /**
     * Additional packages to scan.
     */
    String[] basePackages() default {};
}
