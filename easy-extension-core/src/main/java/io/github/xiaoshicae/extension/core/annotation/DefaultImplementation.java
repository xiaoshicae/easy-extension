package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as the default (fallback) implementation of the extension points it implements. It answers
 * when no business and no mounted ability implements the extension point. A class may back several
 * extension points; one extension point has at most one default implementation.
 * <p>
 * Every extension point needs one, except an extension point whose methods all return {@code void}: the
 * framework then provides a no-op. In Spring, a {@code @Bean} method may carry the annotation, which is handy
 * for a single-method extension point: {@code @Bean @DefaultImplementation FreightCalc fallback() { return ctx -> TEN; }}.
 * </p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultImplementation {
}
