package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * The outcome of resolving one request parameter: which business matched and which of its abilities are active.
 * Immutable and thread-safe, so it can be handed to other threads.
 * <p>
 * It is a snapshot taken when {@link ExtensionContext#resolve(Object)} ran: every mounted ability's {@code match}
 * was evaluated then. Do not cache a resolution across requests.
 * </p>
 */
public interface Resolution {

    /**
     * The highest-precedence implementation of the extension point: the business or a mounted ability that
     * implements it, else the default implementation. Never fails for a registered extension point.
     *
     * @throws io.github.xiaoshicae.extension.core.exception.ResolutionException if the extension point is not registered
     */
    <E> E first(Class<E> point);

    /**
     * All implementations of the extension point in precedence order, the default implementation last.
     * Never empty: the default implementation is always last.
     */
    <E> List<E> all(Class<E> point);

    ResolveTrace trace();

    <E> ExtensionExplanation<E> explain(Class<E> point);

    default <E, R> R invoke(Class<E> point, Function<E, R> invoker) {
        return invoker.apply(first(point));
    }

    default <E, R> List<R> invokeAll(Class<E> point, Function<E, R> invoker) {
        List<R> results = new ArrayList<>();
        for (E extension : all(point)) {
            results.add(invoker.apply(extension));
        }
        return results;
    }

    default <E, R> R invokeReduce(Class<E> point, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        R result = identity;
        for (E extension : all(point)) {
            result = accumulator.apply(result, invoker.apply(extension));
        }
        return result;
    }
}
