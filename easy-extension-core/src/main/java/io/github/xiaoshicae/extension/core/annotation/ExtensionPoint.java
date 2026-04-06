package io.github.xiaoshicae.extension.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as an extension point.
 * <p>
 * Extension points are contracts that businesses or abilities can implement.
 * The framework resolves the correct implementation at runtime based on
 * business identity, ability activation, and priority.
 * </p>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionPoint {

    /**
     * Applicable scenarios for this extension point.
     * <p>
     * Common values: {@code "create_order"}, {@code "payment"}, {@code "fulfillment"}, {@code "after_sale"}.
     * Empty array means no scenario restriction -- the extension point can be invoked in any context.
     * </p>
     * <p>
     * This is a development-time contract hint. The framework does NOT enforce it at runtime.
     * </p>
     *
     * @return scenario identifiers
     */
    String[] scenarios() default {};

    /**
     * Version of this extension point interface.
     * <p>
     * Used for tracking interface evolution. When adding new methods to an extension point,
     * increment the version number and provide {@code default} implementations for backward
     * compatibility.
     * </p>
     * <p>Example:</p>
     * <pre>{@code
     * @ExtensionPoint(version = 2)
     * public interface PaymentExtension {
     *     // v1 method
     *     String pay(OrderContext ctx);
     *
     *     // v2 method - default implementation delegates to v1 for backward compatibility
     *     default PaymentResult payWithOptions(OrderContext ctx, PaymentOptions options) {
     *         return new PaymentResult(pay(ctx));
     *     }
     * }
     * }</pre>
     *
     * @return version number, starting from 1
     */
    int version() default 1;
}
