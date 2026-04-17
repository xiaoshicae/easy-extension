package io.github.xiaoshicae.extension.core.trace;

import java.util.Collections;
import java.util.List;

/**
 * Structured "where would this extension point be routed?" answer for a specific
 * extension point type, computed against the current session state.
 * <p>
 * Unlike {@link ResolveTrace} (which records what happened during session init),
 * an explanation is per-extension-point and tells the caller which candidate
 * will be picked by {@code getFirstMatchedExtension(type)} and why, without
 * actually invoking any business method.
 * </p>
 *
 * @param extensionPointType the queried extension point interface
 * @param scope              the scope this explanation applies to
 * @param candidates         all candidates in priority order (highest priority first);
 *                           each carries whether it implements {@code extensionPointType}
 * @param selected           the first candidate whose implementation is present for
 *                           this type, or {@code null} if none would resolve
 * @param <E>                extension point type
 */
public record ExtensionExplanation<E>(
        Class<E> extensionPointType,
        String scope,
        List<Candidate> candidates,
        Candidate selected
) {

    public ExtensionExplanation {
        candidates = candidates == null ? List.of() : Collections.unmodifiableList(candidates);
    }

    /**
     * A single candidate considered for this extension point.
     *
     * @param code                       implementation code
     * @param priority                   resolved priority (lower = higher priority)
     * @param source                     whether this came from a business / ability / default impl
     * @param implementationClass        actual implementation class if present for this extension
     *                                   point type, otherwise {@code null}
     * @param implementsExtensionPoint   whether the candidate implements the queried type
     */
    public record Candidate(
            String code,
            Integer priority,
            ResolveTrace.EntryType source,
            Class<?> implementationClass,
            boolean implementsExtensionPoint
    ) {
        @Override
        public String toString() {
            return "%s(%s, prio=%d, impl=%s)".formatted(
                    source.label(), code, priority,
                    implementsExtensionPoint ? implementationClass.getSimpleName() : "<none>");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ExtensionExplanation{type=").append(extensionPointType.getSimpleName())
          .append(", scope=").append(scope)
          .append(", selected=").append(selected == null ? "<none>" : selected.code())
          .append(", candidates=[");
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(candidates.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }
}
