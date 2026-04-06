package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a business identity.
 * <p>
 * A business represents a specific tenant, merchant, or business line.
 * It can declare which scenarios it handles, which abilities it uses,
 * and implement extension point interfaces for customization.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Business {

    /**
     * Code of business.
     */
    String code();

    /**
     * Priority of business,
     * compare with priority of abilities used by business.
     */
    int priority() default 0;

    /**
     * Abilities used by business,
     * item format:
     * <p>
     * <code>
     * ${abilityCode}[::${priority}], e.g. {"abilityX", "abilityY::10"}
     * </code>
     * </p>
     * Priority of ability used by business is used to
     * compare with priority of other abilities used by business or business's own priority.
     */
    String[] abilities() default {};
}
