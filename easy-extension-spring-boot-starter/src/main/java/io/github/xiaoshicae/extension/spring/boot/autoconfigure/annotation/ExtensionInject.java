package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the proxy of an extension point into a field of a Spring bean: a field of an extension point type gets the
 * proxy that calls the first matched implementation, a {@code List<ExtensionPoint>} field the proxy that calls all
 * matched implementations.
 * <p>
 * Only fields are injected (non-static, declared in the bean's class or one of its superclasses). The annotation has
 * no effect on a parameter, a constructor parameter included.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionInject {
}
