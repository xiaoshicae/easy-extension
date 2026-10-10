package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.internal.ExtensionProxies;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Entry point of the framework. Immutable once built, so it can be shared freely between threads.
 * <p>
 * <b>Binding.</b> Extension points are called through proxies or {@link #first(Class)}, which pick the implementation
 * from the {@link Resolution} <em>bound to the current thread</em>. Bind once where a unit of work starts (a request, a
 * message, a job) and every call below it sees the same business and abilities:
 * </p>
 * <pre>{@code
 * context.runWith(param, () -> service.handle());                 // bind, run, unbind
 * String s = context.callWith(param, () -> service.compute());    // the same, returning a value
 * try (Binding b = context.bind(param)) { ... }                   // the same, for bodies that throw checked exceptions
 * }</pre>
 * <p>
 * A binding does not follow a call onto another thread: hand it over with {@link #wrap(Runnable)},
 * {@link #executor(Executor)}, or by passing {@link #current()} along and binding it there.
 * Integrations bind for you at the entry points they know (the Spring Boot starter does it for Spring MVC requests).
 * </p>
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
     * @throws io.github.xiaoshicae.extension.core.exception.ResolutionException if more than one business matches, or,
     *                                                                           in strict mode, if none does
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
     * Whether a resolution is bound to the current thread, i.e. whether {@link #current()} would succeed. The default
     * implementation relies on {@link #current()} throwing a {@link ResolutionException} with reason {@code NO_BINDING}
     * when nothing is bound, as documented there; an implementation that behaves differently should override this.
     */
    default boolean isBound() {
        try {
            current();
            return true;
        } catch (ResolutionException e) {
            if (e.reason() == ResolutionException.Reason.NO_BINDING) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Resolves {@code param}, binds it to the current thread while {@code body} runs, then restores the previous binding,
     * also when {@code body} throws. Bindings nest, so this can be used inside an already bound call to act as another
     * business for a while. For a body that throws checked exceptions, use {@code try (Binding b = bind(param)) {...}}.
     *
     * @throws io.github.xiaoshicae.extension.core.exception.ResolutionException if more than one business matches, or,
     *                                                                           in strict mode, if none does
     */
    default void runWith(T param, Runnable body) {
        Objects.requireNonNull(body, "body");
        try (Binding ignored = bind(param)) {
            body.run();
        }
    }

    /** Like {@link #runWith(Object, Runnable)}, returning what {@code body} returns. */
    default <R> R callWith(T param, Supplier<R> body) {
        Objects.requireNonNull(body, "body");
        try (Binding ignored = bind(param)) {
            return body.get();
        }
    }

    /**
     * Binds an existing resolution (e.g. one captured with {@link #current()} on another thread) while {@code body}
     * runs, then restores the previous binding.
     */
    default void runWith(Resolution resolution, Runnable body) {
        Objects.requireNonNull(body, "body");
        try (Binding ignored = bind(resolution)) {
            body.run();
        }
    }

    /** Like {@link #runWith(Resolution, Runnable)}, returning what {@code body} returns. */
    default <R> R callWith(Resolution resolution, Supplier<R> body) {
        Objects.requireNonNull(body, "body");
        try (Binding ignored = bind(resolution)) {
            return body.get();
        }
    }

    /**
     * Hands the binding of the calling thread over to the thread that runs {@code task}: the task runs with the
     * resolution that is bound <em>now</em>, on whichever thread executes it, and the binding there ends with the task.
     * If nothing is bound now, the task is returned unchanged.
     * <pre>{@code
     * pool.submit(context.wrap(() -> notifier.send()));
     * }</pre>
     * The task sees the resolution as it was when it was wrapped, even if the original call has finished by then.
     */
    default Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (!isBound()) {
            return task;
        }
        Resolution resolution = current();
        return () -> runWith(resolution, task);
    }

    /**
     * An executor that runs every task with the resolution bound on the thread that <em>submits</em> it (see
     * {@link #wrap(Runnable)}), e.g. {@code CompletableFuture.supplyAsync(supplier, context.executor(pool))}.
     */
    default Executor executor(Executor delegate) {
        Objects.requireNonNull(delegate, "delegate");
        return task -> delegate.execute(wrap(task));
    }

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
