package io.github.xiaoshicae.extension.core.business;

/**
 * What to do when a request matches more than one business.
 * <p>
 * Independent of {@link UnknownBusinessPolicy}; see there.
 * </p>
 *
 * @since 4.0
 */
public enum MultiMatchPolicy {

    /**
     * Fail the session initialization with {@code multiple business found}.
     * Right when business matchers are meant to be mutually exclusive: overlap is a configuration mistake
     * that should surface immediately.
     */
    REJECT,

    /**
     * Let the {@link BusinessMatchSelector} pick one. A WARN is logged once per distinct set of matching
     * businesses, because overlap is usually unintended and silently picking one hides it.
     */
    SELECT
}
