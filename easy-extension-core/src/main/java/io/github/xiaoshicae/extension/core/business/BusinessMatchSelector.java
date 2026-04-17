package io.github.xiaoshicae.extension.core.business;

import java.util.List;

/**
 * Strategy for picking a single winning business when more than one business matches
 * the current request (non-strict mode).
 * <p>
 * The framework provides {@link OrderedCodeBusinessMatchSelector}, which matches by
 * configured code list; applications with more exotic needs (per-tenant hierarchy,
 * grayscale tags read from the matcher param, regex on code, etc.) can supply their
 * own implementation.
 * </p>
 *
 * @param <T> matcher param type
 */
@FunctionalInterface
public interface BusinessMatchSelector<T> {

    /**
     * Pick one business from the candidates. Implementations must not return a
     * business that is not in {@code matchedBusinesses}; returning {@code null}
     * is allowed and behaves the same as "no business matched" (falls back to
     * default implementation).
     *
     * @param matchedBusinesses all businesses whose {@code match(param)} returned true,
     *                          in registration order; guaranteed non-empty
     * @param param             the matcher param used for this resolution
     * @return selected business, or {@code null} for "none"
     */
    IBusiness<T> select(List<IBusiness<T>> matchedBusinesses, T param);
}
