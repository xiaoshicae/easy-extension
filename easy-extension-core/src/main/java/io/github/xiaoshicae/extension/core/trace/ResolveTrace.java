package io.github.xiaoshicae.extension.core.trace;

import java.util.List;

/**
 * How one {@code resolve(param)} came out: which business matched and the ordered chain of the business and
 * the abilities it mounted. Default implementations are per extension point and not part of the chain, see
 * {@link ExtensionExplanation}.
 *
 * @param matchedBusinessCode code of the matched business, {@code null} if none matched
 * @param chain               business and matched abilities in precedence order
 * @param skippedAbilities    mounted abilities whose {@code match} returned {@code false}
 * @param costNanos           time taken to resolve
 */
public record ResolveTrace(String matchedBusinessCode, List<ChainEntry> chain,
                           List<SkippedAbility> skippedAbilities, long costNanos) {

    public ResolveTrace {
        chain = List.copyOf(chain);
        skippedAbilities = List.copyOf(skippedAbilities);
    }

    public long costMillis() {
        return costNanos / 1_000_000;
    }

    /**
     * @param position index of the slot in the business's {@code abilities} order (the business itself included)
     */
    public record ChainEntry(String code, EntryType type, int position) {
        @Override
        public String toString() {
            return "%s(%s, position=%d)".formatted(type.label(), code, position);
        }
    }

    public record SkippedAbility(String code, int position, String reason) {
    }

    public enum EntryType {
        BUSINESS("business"),
        ABILITY("ability"),
        DEFAULT("default");

        private final String label;

        EntryType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
