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
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultImplementation {
}
