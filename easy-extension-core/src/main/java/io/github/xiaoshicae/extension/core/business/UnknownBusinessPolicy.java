package io.github.xiaoshicae.extension.core.business;

/**
 * What to do when a request matches no business at all.
 * <p>
 * Independent of {@link MultiMatchPolicy}: "nobody matched" and "several matched" are different situations and
 * deserve different answers. (Earlier versions had a single strict/non-strict switch for both.)
 * </p>
 *
 * @since 3.4
 */
public enum UnknownBusinessPolicy {

    /**
     * Fail the session initialization with {@code no business matched}.
     * Right when every legitimate request belongs to some business; an unknown one is a bug or an attack.
     */
    REJECT,

    /**
     * Carry on without a business: the extension points are served by the default implementations.
     * Right when there is a sensible baseline behavior for requests nobody has customised.
     */
    DEFAULT
}
