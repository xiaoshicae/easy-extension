package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.ExtensionScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scans for extension points ({@code @ExtensionPoint}), the matcher parameter type ({@code @MatcherParam}), abilities,
 * businesses and default implementations, and registers them with the extension context.
 * <p>
 * The package of the annotated class is always scanned. {@code @Profile} and {@code @Conditional...} on a scanned class
 * are decided like they are for any other component of the application. A class that the application's own
 * {@code @ComponentScan} finds as well (a business that is also a {@code @Component}) is registered once.
 * </p>
 * <p>
 * An ability, business or default implementation is picked up where its class is inside a scanned package. An instance
 * that the application creates itself, with a {@code @Bean} method, is registered with the extension context only if it
 * is an {@code IAbility}, {@code IBusiness} or {@code IExtensionPointGroupDefaultImplementation}; its class being
 * annotated is not enough.
 * </p>
 */
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
