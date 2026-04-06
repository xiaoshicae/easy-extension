package io.github.xiaoshicae.extension.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured trace of the extension point resolution process.
 * <p>
 * Captures which business matched, which abilities were activated or skipped,
 * and the final resolution chain ordered by priority.
 * </p>
 * <p>
 * This is useful for debugging in production: "why did this order go to
 * MerchantA's logic instead of MerchantB's?"
 * </p>
 */
public class ResolveTrace {

    private final String scope;
    private final String matchedBusinessCode;
    private final Integer matchedBusinessPriority;
    private final List<AbilityTraceItem> abilities;
    private final String defaultImplCode;
    private final Integer defaultImplPriority;
    private final long costMillis;

    private ResolveTrace(Builder builder) {
        this.scope = builder.scope;
        this.matchedBusinessCode = builder.matchedBusinessCode;
        this.matchedBusinessPriority = builder.matchedBusinessPriority;
        this.abilities = Collections.unmodifiableList(builder.abilities);
        this.defaultImplCode = builder.defaultImplCode;
        this.defaultImplPriority = builder.defaultImplPriority;
        this.costMillis = builder.costMillis;
    }

    public static Builder builder(String scope) {
        return new Builder(scope);
    }

    /**
     * Scope of this resolution (e.g., default scope or custom scope name).
     */
    public String getScope() {
        return scope;
    }

    /**
     * Code of the matched business, or null if no business matched.
     */
    public String getMatchedBusinessCode() {
        return matchedBusinessCode;
    }

    /**
     * Priority of the matched business.
     */
    public Integer getMatchedBusinessPriority() {
        return matchedBusinessPriority;
    }

    /**
     * List of abilities evaluated during this resolution, in declaration order.
     */
    public List<AbilityTraceItem> getAbilities() {
        return abilities;
    }

    /**
     * Code of the default implementation (always present as fallback).
     */
    public String getDefaultImplCode() {
        return defaultImplCode;
    }

    /**
     * Priority of the default implementation.
     */
    public Integer getDefaultImplPriority() {
        return defaultImplPriority;
    }

    /**
     * Time taken to resolve this session.
     */
    public long getCostMillis() {
        return costMillis;
    }

    /**
     * Returns the final resolution chain: all matched codes ordered by priority
     * (ascending, lowest number = highest priority).
     */
    public List<ResolutionEntry> getResolutionChain() {
        List<ResolutionEntry> chain = new ArrayList<>();
        if (matchedBusinessCode != null) {
            chain.add(new ResolutionEntry(matchedBusinessCode, matchedBusinessPriority, EntryType.BUSINESS));
        }
        for (AbilityTraceItem ability : abilities) {
            if (ability.matched) {
                chain.add(new ResolutionEntry(ability.code, ability.priority, EntryType.ABILITY));
            }
        }
        chain.add(new ResolutionEntry(defaultImplCode, defaultImplPriority, EntryType.DEFAULT));
        // Sort by priority ascending (lowest number = highest priority)
        chain.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
        return Collections.unmodifiableList(chain);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResolveTrace{scope='").append(scope).append("', ");
        if (matchedBusinessCode != null) {
            sb.append("business=").append(matchedBusinessCode)
              .append("(priority=").append(matchedBusinessPriority).append("), ");
        } else {
            sb.append("business=none, ");
        }
        sb.append("abilities=[");
        for (int i = 0; i < abilities.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(abilities.get(i));
        }
        sb.append("], default=").append(defaultImplCode)
          .append("(priority=").append(defaultImplPriority).append("), ");
        sb.append("cost=").append(costMillis).append("ms}");
        return sb.toString();
    }

    /**
     * A single ability evaluated during resolution.
     *
     * @param code        ability code
     * @param priority    priority assigned by the business
     * @param matched     whether this ability matched for the given parameter
     * @param skippedReason reason if skipped (null if matched or not skipped)
     */
    public record AbilityTraceItem(String code, Integer priority, boolean matched, String skippedReason) {
        public static AbilityTraceItem matched(String code, Integer priority) {
            return new AbilityTraceItem(code, priority, true, null);
        }

        public static AbilityTraceItem skipped(String code, Integer priority, String reason) {
            return new AbilityTraceItem(code, priority, false, reason);
        }
    }

    /**
     * A single entry in the resolution chain.
     *
     * @param code     code of the implementation
     * @param priority priority value
     * @param type     type of the entry
     */
    public record ResolutionEntry(String code, Integer priority, EntryType type) {
        @Override
        public String toString() {
            return "%s(%s, prio=%d)".formatted(type.label, code, priority);
        }
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

    public static class Builder {
        private final String scope;
        private String matchedBusinessCode;
        private Integer matchedBusinessPriority;
        private final List<AbilityTraceItem> abilities = new ArrayList<>();
        private String defaultImplCode;
        private Integer defaultImplPriority;
        private long costMillis;

        private Builder(String scope) {
            this.scope = scope;
        }

        public Builder matchedBusiness(String code, Integer priority) {
            this.matchedBusinessCode = code;
            this.matchedBusinessPriority = priority;
            return this;
        }

        public Builder abilityMatched(String code, Integer priority) {
            this.abilities.add(AbilityTraceItem.matched(code, priority));
            return this;
        }

        public Builder abilitySkipped(String code, Integer priority, String reason) {
            this.abilities.add(AbilityTraceItem.skipped(code, priority, reason));
            return this;
        }

        public Builder defaultImpl(String code, Integer priority) {
            this.defaultImplCode = code;
            this.defaultImplPriority = priority;
            return this;
        }

        public Builder costMillis(long millis) {
            this.costMillis = millis;
            return this;
        }

        public ResolveTrace build() {
            return new ResolveTrace(this);
        }
    }
}
