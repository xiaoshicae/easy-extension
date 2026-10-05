package io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a session-aware extension point proxy.
 * <ul>
 *   <li>{@code @ExtensionInject MyExtension ext} resolves, on every call, to the first matched
 *   implementation of the current session.</li>
 *   <li>{@code @ExtensionInject List<MyExtension> exts} (the element type is required) resolves to all
 *   matched implementations of the current session, in priority order.</li>
 * </ul>
 * Works on fields as well as constructor and method parameters. The injected proxy already resolves on every
 * call, so {@code @Lazy} has no additional effect on it. Parameters of any other type ({@code Set<X>},
 * {@code Optional<X>}, {@code ObjectProvider<X>}, ...) are injected by Spring as usual, ignoring this annotation.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionInject {
}
