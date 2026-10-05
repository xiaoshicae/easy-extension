package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Entry point of the framework. Immutable once built, so it can be shared freely between threads.
 *
 * @param <T> type of the request parameter that businesses and abilities match on
 */
public interface ExtensionContext<T> {

    static <T> ExtensionContextBuilder<T> builder() {
        return new ExtensionContextBuilder<>();
    }

    /**
     * Resolves the parameter without binding it to any thread.
     *
     * @throws io.github.xiaoshicae.extension.core.exception.ResolutionException in strict mode, if no business or more
     *                                                                           than one business matches
     */
    Resolution resolve(T param);

    /**
     * Resolves the parameter and binds the result to the current thread until the returned binding is closed.
     */
    Binding bind(T param);

    /**
     * Binds an existing resolution to the current thread, e.g. to continue a request on another thread.
     */
    Binding bind(Resolution resolution);

    /**
     * The resolution bound to the current thread.
     *
     * @throws io.github.xiaoshicae.extension.core.exception.ResolutionException with reason {@code NO_BINDING}
     */
    Resolution current();

    /**
     * Drops every binding of the current thread. A safety net for thread pools; prefer closing bindings.
     */
    void clear();

    default <E> E first(Class<E> point) {
        return current().first(point);
    }

    default <E> List<E> all(Class<E> point) {
        return current().all(point);
    }

    default <E, R> R invoke(Class<E> point, Function<E, R> invoker) {
        return current().invoke(point, invoker);
    }

    default <E, R> List<R> invokeAll(Class<E> point, Function<E, R> invoker) {
        return current().invokeAll(point, invoker);
    }

    default <E, R> R invokeReduce(Class<E> point, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        return current().invokeReduce(point, invoker, identity, accumulator);
    }

    /**
     * A proxy of the extension point that, on every call, forwards to {@code current().first(point)}.
     * Exceptions of the implementation are rethrown as they are.
     */
    <E> E proxy(Class<E> point);

    /**
     * A read-only list view that always reflects {@code current().all(point)}.
     */
    <E> List<E> proxyAll(Class<E> point);

    ExtensionCatalog catalog();
}
