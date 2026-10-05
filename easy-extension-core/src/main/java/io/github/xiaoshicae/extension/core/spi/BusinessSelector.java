package io.github.xiaoshicae.extension.core.spi;

import java.util.List;

/**
 * Picks one business when more than one matches (only consulted when the context is not strict).
 *
 * @param <T> matcher param type
 */
@FunctionalInterface
public interface BusinessSelector<T> {

    /**
     * @param matchedBusinessCodes codes of all businesses that matched, in registration order; at least two
     * @param param                the request parameter
     * @return one of {@code matchedBusinessCodes}, or {@code null} for "no business" (default implementations only)
     */
    String select(List<String> matchedBusinessCodes, T param);
}
