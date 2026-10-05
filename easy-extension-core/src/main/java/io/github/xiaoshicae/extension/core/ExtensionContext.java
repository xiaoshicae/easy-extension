package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.internal.ExtensionProxies;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Entry point of the framework. Immutable once built, so it can be shared freely between threads.
 *
 * @param <T> type of the request parameter that businesses and abilities match on
 */
public interface ExtensionContext<T> {

    /** Starts assembling a context; {@code build()} validates everything at once. */
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

    /** Shortcut for {@code current().first(point)}. */
    default <E> E first(Class<E> point) {
        return current().first(point);
    }

    /** Shortcut for {@code current().all(point)}. */
    default <E> List<E> all(Class<E> point) {
        return current().all(point);
    }

    /** Shortcut for {@code current().invoke(point, invoker)}. */
    default <E, R> R invoke(Class<E> point, Function<E, R> invoker) {
        return current().invoke(point, invoker);
    }

    /** Shortcut for {@code current().invokeAll(point, invoker)}. */
    default <E, R> List<R> invokeAll(Class<E> point, Function<E, R> invoker) {
        return current().invokeAll(point, invoker);
    }

    /** Shortcut for {@code current().invokeReduce(point, invoker, identity, accumulator)}. */
    default <E, R> R invokeReduce(Class<E> point, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        return current().invokeReduce(point, invoker, identity, accumulator);
    }

    /**
     * A proxy of the extension point that, on every call, forwards to {@code current().first(point)}.
     * Exceptions of the implementation are rethrown as they are.
     */
    default <E> E proxy(Class<E> point) {
        return ExtensionProxies.proxy(this, point);
    }

    /**
     * A read-only list view that always reflects {@code current().all(point)}.
     */
    default <E> List<E> proxyAll(Class<E> point) {
        return ExtensionProxies.proxyAll(this, point);
    }

    /** Read-only description of everything registered in this context. */
    ExtensionCatalog catalog();
}
