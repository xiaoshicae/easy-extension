package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.SessionException;

public interface ISessionManager<T> {

    /**
     * Init session before process.
     *
     * @param param for business or ability match test
     */
    void initSession(T param) throws SessionException;

    /**
     * Remove session after process.
     */
    void removeSession();
}
