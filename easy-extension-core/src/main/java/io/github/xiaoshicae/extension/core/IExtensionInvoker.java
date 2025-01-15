package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;

import java.util.List;
import java.util.function.Function;

public interface IExtensionInvoker {

    /**
     * Invoke first matched extension with lambada func.
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     */
    <E, R> R invoke(Class<E> extensionType, Function<E, R> invoker) throws QueryException;


    /**
     * Invoke all matched extension with lambada func.
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     */
    <E, R> List<R> invokeAll(Class<E> extensionType, Function<E, R> invoker) throws QueryException;

    /**
     * Invoke scoped first matched extension with lambada func.
     *
     * @param scope         namespace of
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of scoped extension exec
     */
    <E, R> R scopedInvoke(String scope, Class<E> extensionType, Function<E, R> invoker) throws QueryException;

    /**
     * Invoke scoped all matched extension with lambada func.
     *
     * @param extensionType extension point class
     * @param invoker       lambada func
     * @return result of extension exec
     */
    <E, R> List<R> scopedInvokeAll(String scope, Class<E> extensionType, Function<E, R> invoker) throws QueryException;
}
