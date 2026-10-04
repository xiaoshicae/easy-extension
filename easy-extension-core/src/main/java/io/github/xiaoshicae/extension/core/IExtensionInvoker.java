package io.github.xiaoshicae.extension.core;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public interface IExtensionInvoker {

    /**
     * Invoke first matched extension with lambada func.
     * <br>
     * e.g.
     * <blockquote><pre>{@code
     * interface Ext {
     *     String doSomething();
     * }
     *
     * String res = IExtensionInvoker.invoke(Ext.class, e -> e.doSomething());
     * }</pre></blockquote>
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if extension not found
     */
    <E, R> R invoke(Class<E> extensionType, Function<E, R> invoker);


    /**
     * Invoke all matched extension with lambada func.
     * <br>
     * e.g.
     * <blockquote><pre>{@code
     * interface Ext {
     *     String doSomething();
     * }
     *
     * List<String> resList = IExtensionInvoker.invokeAll(Ext.class, e -> e.doSomething());
     * }</pre></blockquote>
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if extension not found
     */
    <E, R> List<R> invokeAll(Class<E> extensionType, Function<E, R> invoker);

    /**
     * Scope-aware overload of {@link #invoke(Class, Function)}.
     *
     * @param scope         namespace of session
     * @param extensionType extension point class
     * @param invoker       lambda applied to the first matched extension
     * @return result of extension exec
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if scoped extension not found
     */
    <E, R> R invoke(String scope, Class<E> extensionType, Function<E, R> invoker);

    /**
     * Scope-aware overload of {@link #invokeAll(Class, Function)}.
     *
     * @param scope         namespace of session
     * @param extensionType extension point class
     * @param invoker       lambda applied to each matched extension
     * @return per-extension results
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if scoped extension not found
     */
    <E, R> List<R> invokeAll(String scope, Class<E> extensionType, Function<E, R> invoker);

    /**
     * @deprecated Use {@link #invoke(String, Class, Function)}.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default <E, R> R scopedInvoke(String scope, Class<E> extensionType, Function<E, R> invoker) {
        return invoke(scope, extensionType, invoker);
    }

    /**
     * @deprecated Use {@link #invokeAll(String, Class, Function)}.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default <E, R> List<R> scopedInvokeAll(String scope, Class<E> extensionType, Function<E, R> invoker) {
        return invokeAll(scope, extensionType, invoker);
    }

    /**
     * Invoke all matched extensions and reduce results into a single value.
     * <br>
     * e.g.
     * <blockquote><pre>{@code
     * // Sum all promotion discounts from matched abilities and business
     * BigDecimal totalDiscount = invokeReduce(
     *     PromotionCalcExtension.class,
     *     e -> e.calcPromotion(ctx),
     *     BigDecimal.ZERO,
     *     BigDecimal::add
     * );
     * }</pre></blockquote>
     *
     * @param extensionType extension point class
     * @param invoker       lambda to invoke on each matched extension
     * @param identity      initial value for the reduction
     * @param accumulator   function to combine two results
     * @return reduced result
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if extension not found
     */
    <E, R> R invokeReduce(Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator);

    /**
     * Scope-aware overload of
     * {@link #invokeReduce(Class, Function, Object, BinaryOperator)}.
     */
    <E, R> R invokeReduce(String scope, Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator);

    /**
     * @deprecated Use {@link #invokeReduce(String, Class, Function, Object, BinaryOperator)}.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default <E, R> R scopedInvokeReduce(String scope, Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        return invokeReduce(scope, extensionType, invoker, identity, accumulator);
    }
}
