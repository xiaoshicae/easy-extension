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
     * Invoke scoped first matched extension with lambada func.
     * <br>
     * e.g.
     * <blockquote><pre>{@code
     * interface Ext {
     *     String doSomething();
     * }
     *
     * List<String> resList = IExtensionInvoker.scopedInvoke("scope", Ext.class, e -> e.doSomething());
     * }</pre></blockquote>
     *
     * @param scope         namespace of
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of scoped extension exec
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if scoped extension not found
     */
    <E, R> R scopedInvoke(String scope, Class<E> extensionType, Function<E, R> invoker);

    /**
     * Invoke scoped all matched extension with lambada func.
     * <br>
     * e.g.
     * <blockquote><pre>{@code
     * interface Ext {
     *     String doSomething();
     * }
     *
     * List<String> resList = IExtensionInvoker.scopedInvokeAll("scope", Ext.class, e -> e.doSomething());
     * }</pre></blockquote>
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if scoped extension not found
     */
    <E, R> List<R> scopedInvokeAll(String scope, Class<E> extensionType, Function<E, R> invoker);

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
     * Invoke all matched scoped extensions and reduce results into a single value.
     *
     * @param scope         namespace
     * @param extensionType extension point class
     * @param invoker       lambda to invoke on each matched extension
     * @param identity      initial value for the reduction
     * @param accumulator   function to combine two results
     * @return reduced result
     * @throws io.github.xiaoshicae.extension.core.exception.InvokeException if scoped extension not found
     */
    <E, R> R scopedInvokeReduce(String scope, Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator);
}
