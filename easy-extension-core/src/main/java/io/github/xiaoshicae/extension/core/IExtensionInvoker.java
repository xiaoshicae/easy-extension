package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.InvokeException;

import java.util.List;
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
     * @throws InvokeException if extension not found
     */
    <E, R> R invoke(Class<E> extensionType, Function<E, R> invoker) throws InvokeException;


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
     * @throws InvokeException if extension not found
     */
    <E, R> List<R> invokeAll(Class<E> extensionType, Function<E, R> invoker) throws InvokeException;

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
     * @throws InvokeException if scoped extension not found
     */
    <E, R> R scopedInvoke(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException;

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
     * @throws InvokeException if scoped extension not found
     */
    <E, R> List<R> scopedInvokeAll(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException;
}
