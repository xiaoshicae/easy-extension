package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public interface as an extension point: a contract that businesses and abilities implement.
 * <p>
 * The framework resolves, per request, which implementation answers a call: the matched business,
 * the abilities it mounts (in the order of {@link Business#uses()}), then the default implementation.
 * Every extension point has a default implementation: a {@link DefaultImplementation}, or, when all its methods
 * return {@code void}, a no-op the framework provides.
 * An extension point is recognized wherever it appears in the type hierarchy of an implementation
 * (superclass, interface, or an interface's parent interface).
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionPoint {

    /**
     * Applicable scenarios, e.g. {@code "create_order"}. Documentation only, shown by the admin console;
     * the framework does not enforce it.
     */
    String[] scenarios() default {};

    /**
     * Version of this extension point, documentation only. Evolve an extension point by adding
     * {@code default} methods.
     */
    int version() default 1;
}
