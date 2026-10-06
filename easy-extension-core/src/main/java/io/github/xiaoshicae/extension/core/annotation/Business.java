package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a business: an integration party (tenant, merchant, business line) that mounts
 * abilities and may implement extension points itself.
 * <p>
 * The class must implement {@link io.github.xiaoshicae.extension.core.interfaces.Matcher} unless the
 * context uses a {@link io.github.xiaoshicae.extension.core.spi.BusinessResolver}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Business {

    /**
     * Unique code of the business. Defaults to the fully qualified class name.
     */
    String code() default "";

    /**
     * Abilities mounted by this business, and the position of the business itself, as a precedence order:
     * earlier entries win. List {@link Self} to place the business's own implementation; if it is not
     * listed, the business comes first.
     * <pre>{@code
     * abilities = { FreeShipping.class }                  // business, then FreeShipping
     * abilities = { FreeShipping.class, Self.class }      // FreeShipping overrides the business
     * }</pre>
     */
    Class<?>[] abilities() default {};
}
