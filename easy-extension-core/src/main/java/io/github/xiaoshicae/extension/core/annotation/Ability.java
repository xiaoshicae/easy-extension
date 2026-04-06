package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Ability {

    /**
     * Code of ability.
     */
    String code();

    /**
     * Ability codes that this ability requires to be present on the same business.
     * <p>
     * During registration, the framework will throw an error if a business mounts this ability
     * without also mounting all required abilities.
     * </p>
     *
     * @return required ability codes
     */
    String[] requires() default {};

    /**
     * Ability codes that are mutually exclusive with this ability.
     * <p>
     * During registration, the framework will throw an error if a business mounts both
     * this ability and any of the excluded abilities.
     * </p>
     *
     * @return excluded ability codes
     */
    String[] excludes() default {};
}
