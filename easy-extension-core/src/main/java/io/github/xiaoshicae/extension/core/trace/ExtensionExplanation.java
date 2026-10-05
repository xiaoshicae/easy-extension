package io.github.xiaoshicae.extension.core.trace;

import java.util.Collections;
import java.util.List;

/**
 * Which implementation {@code first(extensionPointType)} would pick and why, without calling anything.
 *
 * @param extensionPointType the queried extension point
 * @param candidates         the chain in precedence order, then the default implementation if there is one; each says
 *                           whether it implements {@code extensionPointType}
 * @param selected           the first candidate that implements it, {@code null} if none would answer
 * @param <E>                extension point type
 */
public record ExtensionExplanation<E>(Class<E> extensionPointType, List<Candidate> candidates, Candidate selected) {

    public ExtensionExplanation {
        candidates = candidates == null ? List.of() : Collections.unmodifiableList(candidates);
    }

    /**
     * @param position                 slot in the business's {@code uses} order; the default implementation follows the chain
     * @param implementationClass      the user's class behind the candidate
     * @param implementsExtensionPoint whether the candidate implements the queried extension point
     */
    public record Candidate(String code, int position, ResolveTrace.EntryType source, Class<?> implementationClass,
                            boolean implementsExtensionPoint) {
        @Override
        public String toString() {
            return "%s(%s, position=%d, impl=%s)".formatted(source.label(), code, position,
                    implementsExtensionPoint ? implementationClass.getSimpleName() : "<none>");
        }
    }
}
