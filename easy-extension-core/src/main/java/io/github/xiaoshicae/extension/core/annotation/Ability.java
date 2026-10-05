package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an ability: a reusable implementation of one or more extension points that
 * businesses mount through {@link Business#uses()}.
 * <p>
 * The class must implement {@link io.github.xiaoshicae.extension.core.interfaces.Matcher}; a mounted
 * ability only takes part in a request when its {@code match} returns {@code true}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Ability {

    /**
     * Unique code of the ability. Defaults to the fully qualified class name.
     */
    String code() default "";

    /**
     * Abilities that must be mounted together with this one on the same business.
     * Only presence is checked, not order.
     */
    Class<?>[] requires() default {};

    /**
     * Abilities that must not be mounted together with this one on the same business.
     */
    Class<?>[] excludes() default {};
}
