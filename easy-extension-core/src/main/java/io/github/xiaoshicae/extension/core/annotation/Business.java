package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Business {

    /**
     * Code of business.
     */
    String code();

    /**
     * Priority of extension point that implements by business,
     * priority is used to compare with abilities
     */
    int priority() default 0;

    /**
     * Abilities used by business,
     * item format:<br>
     * <code>
     * ${abilityCode}[::${priority}]<br>
     * i.e {"abilityX", "abilityY::10"}
     * </code>
     * <br>
     * priority is used to compare with priority of other ability or business
     */
    String[] abilities() default {};
}
