package io.github.xiaoshicae.extension.core.spi;

import java.util.List;

/**
 * Default {@link BusinessSelector}: the matched business whose code comes first in a configured order list;
 * the first registered one when none of them is listed.
 *
 * @param <T> matcher param type
 */
public final class OrderedCodeBusinessSelector<T> implements BusinessSelector<T> {

    private final List<String> order;

    /** Selects by the position in {@code order}; codes not listed come after the listed ones, in registration order. */
    public OrderedCodeBusinessSelector(List<String> order) {
        this.order = order == null ? List.of() : List.copyOf(order);
    }

    @Override
    public String select(List<String> matchedBusinessCodes, T param) {
        String best = null;
        int bestPosition = Integer.MAX_VALUE;
        for (String code : matchedBusinessCodes) {
            int position = order.indexOf(code);
            if (position >= 0 && position < bestPosition) {
                bestPosition = position;
                best = code;
            }
        }
        return best != null ? best : matchedBusinessCodes.get(0);
    }
}
