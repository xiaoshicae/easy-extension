package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;

import org.springframework.context.annotation.Import;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.ExtensionScannerRegistrar;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ExtensionScannerRegistrar.class)
public @interface ExtensionScan {
    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
     * {@code @ExtensionScan("org.my.pkg")} instead of {@code @ExtensionScan(basePackages = "org.my.pkg"})}.
     *
     * @return base package names
     */
    String[] value() default {};

    /**
     * Base packages to scan for Extension interfaces. Note that only interfaces with at least one method will be
     * registered; concrete classes will be ignored.
     *
     * @return base package names for scanning extension interface
     */
    String[] basePackages() default {};
}
