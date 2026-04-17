package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

public interface IExtensionSession<T> {

    /**
     * Init session before process on the default scope.
     *
     * @param param for business or ability match test
     * @throws SessionException if business miss match or multi match when strict enabled
     */
    void initSession(T param) throws SessionException;

    /**
     * Scope-aware overload of {@link #initSession(Object)}.
     *
     * @param scope namespace of session
     * @param param for business or ability match test
     * @throws SessionException if business miss match or multi match when strict enabled
     */
    void initSession(String scope, T param) throws SessionException;

    /**
     * @deprecated Use {@link #initSession(String, Object)} — the scoped/non-scoped
     *             pair has been collapsed into a single overload. Will be removed
     *             in a future release.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default void initScopedSession(String scope, T param) throws SessionException {
        initSession(scope, param);
    }

    /**
     * Remove all session (include scoped session) after process.
     */
    void removeSession();

    /**
     * Get the resolve trace of the most recent session initialization.
     * <p>
     * Returns null if no session has been initialized yet.
     * The trace captures which business matched, which abilities were
     * activated or skipped, and the final resolution chain.
     * </p>
     *
     * @return the resolve trace, or null if not available
     */
    ResolveTrace getLastResolveTrace();

    /**
     * Explain, without invoking any business method, which implementation would
     * be selected by {@code getFirstMatchedExtension(extensionPointType)} right now
     * and why. Useful for diagnostics and Admin UI "resolve" views.
     * <p>
     * Requires that a session has been initialized; otherwise throws
     * {@link io.github.xiaoshicae.extension.core.exception.SessionException}.
     * </p>
     *
     * @param extensionPointType the extension point interface to inspect
     * @return structured explanation with candidates and the selected one (if any)
     * @throws UnsupportedOperationException if this session implementation does not
     *                                       support diagnostics
     */
    default <E> ExtensionExplanation<E> explain(Class<E> extensionPointType) {
        throw new UnsupportedOperationException(
                "explain() is not supported by this session implementation");
    }

    /**
     * Scoped variant of {@link #explain(Class)}.
     */
    default <E> ExtensionExplanation<E> explainScoped(String scope, Class<E> extensionPointType) {
        throw new UnsupportedOperationException(
                "explainScoped() is not supported by this session implementation");
    }
}
