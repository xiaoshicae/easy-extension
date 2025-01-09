package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.ExtensionScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ExtensionScannerRegistrar.class)
public @interface ExtensionScan {

    /**
     * Packages to scan for annotated components (extension point, ability, business ...) .
     * @return packages to scan
     */
    String[] scanPackages() default {};
}
