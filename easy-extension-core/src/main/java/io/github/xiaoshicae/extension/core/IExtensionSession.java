package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
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
     * Remove the session of one scope only, leaving the other scopes (the default scope included) untouched.
     * <p>
     * The default implementation removes everything, which is the only thing an implementation that does not
     * distinguish scopes can do.
     * </p>
     *
     * @param scope namespace of session
     * @since 3.4
     */
    default void removeSession(String scope) {
        removeSession();
    }

    /**
     * Resolve who the request is, without binding the result to the current thread: evaluates the business
     * and ability matchers and returns the chain of active implementations.
     * <p>
     * Bind the chain with {@link #bind(ResolvedChain)}, on this thread or on another one. Capturing the chain
     * on the request thread and binding it in a worker avoids evaluating the matchers a second time, and makes sure
     * the worker acts as the same business as the request did.
     * </p>
     *
     * @param param for business or ability match test
     * @return the resolved chain
     * @throws SessionException if business miss match or multi match when the policy rejects it
     * @throws UnsupportedOperationException if this session implementation does not support chains
     * @since 3.4
     */
    default ResolvedChain resolve(T param) throws SessionException {
        throw new UnsupportedOperationException("resolve() is not supported by this session implementation");
    }

    /**
     * The chain bound to the current thread in the default scope, if any.
     *
     * @return the chain, or {@code null} if no session is initialized
     * @throws UnsupportedOperationException if this session implementation does not support chains
     * @since 3.4
     */
    default ResolvedChain currentChain() {
        throw new UnsupportedOperationException("currentChain() is not supported by this session implementation");
    }

    /**
     * Scope-aware overload of {@link #currentChain()}.
     *
     * @param scope namespace of session
     * @since 3.4
     */
    default ResolvedChain currentChain(String scope) {
        throw new UnsupportedOperationException("currentChain() is not supported by this session implementation");
    }

    /**
     * Bind a previously resolved chain to the current thread in the default scope, in place of
     * {@link #initSession(Object)}: nothing is matched, the chain is taken as it is.
     * <p>
     * Refused if the chain was resolved against a different registry, or refers to codes this registry does not
     * know.
     * </p>
     *
     * @param chain a chain obtained from {@link #resolve(Object)} or {@link #currentChain()}
     * @throws SessionException if the chain does not fit this registry
     * @throws UnsupportedOperationException if this session implementation does not support chains
     * @since 3.4
     */
    default void bind(ResolvedChain chain) throws SessionException {
        throw new UnsupportedOperationException("bind() is not supported by this session implementation");
    }

    /**
     * Scope-aware overload of {@link #bind(ResolvedChain)}.
     *
     * @param scope namespace of session
     * @param chain a chain obtained from {@link #resolve(Object)} or {@link #currentChain(String)}
     * @since 3.4
     */
    default void bind(String scope, ResolvedChain chain) throws SessionException {
        throw new UnsupportedOperationException("bind() is not supported by this session implementation");
    }

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
