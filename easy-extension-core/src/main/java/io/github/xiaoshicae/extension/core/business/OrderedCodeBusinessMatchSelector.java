package io.github.xiaoshicae.extension.core.business;

import java.util.List;

/**
 * Default {@link BusinessMatchSelector}: picks the matched business whose code
 * comes first in a configured order list. Falls back to the first matched
 * business (registration order) when the list is empty or no matched business
 * is listed.
 * <p>
 * This reproduces the historical behaviour of {@code easy-extension.business-match-order}.
 * </p>
 *
 * @param <T> matcher param type
 */
public class OrderedCodeBusinessMatchSelector<T> implements BusinessMatchSelector<T> {

    private final List<String> order;

    public OrderedCodeBusinessMatchSelector(List<String> order) {
        this.order = order == null ? List.of() : List.copyOf(order);
    }

    @Override
    public IBusiness<T> select(List<IBusiness<T>> matchedBusinesses, T param) {
        if (matchedBusinesses.isEmpty()) {
            return null;
        }
        if (matchedBusinesses.size() == 1 || order.isEmpty()) {
            return matchedBusinesses.get(0);
        }
        IBusiness<T> best = null;
        int bestPos = Integer.MAX_VALUE;
        for (IBusiness<T> business : matchedBusinesses) {
            int pos = order.indexOf(business.code());
            if (pos >= 0 && pos < bestPos) {
                bestPos = pos;
                best = business;
            }
        }
        return best != null ? best : matchedBusinesses.get(0);
    }
}
