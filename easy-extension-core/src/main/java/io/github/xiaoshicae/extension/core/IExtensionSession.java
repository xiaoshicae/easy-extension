package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.SessionException;

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
}
