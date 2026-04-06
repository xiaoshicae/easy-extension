package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

public interface IExtensionSession<T> {

    /**
     * Init session before process.
     *
     * @param param for business or ability match test
     * @throws SessionException if business miss match or multi match when strict enabled
     */
    void initSession(T param) throws SessionException;

    /**
     * Init scoped session before process.
     *
     * @param param for business or ability match test
     * @throws SessionException if business miss match or multi match when strict enabled
     */
    void initScopedSession(String scope, T param) throws SessionException;

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
}
