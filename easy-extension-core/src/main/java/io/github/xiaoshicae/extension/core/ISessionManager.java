package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.ExtensionException;

public interface ISessionManager<T> {

    /**
     * init session before process
     *
     * @param param used to match business or ability
     */
    void initSession(T param) throws ExtensionException;

    /**
     * remove session after process
     */
    void removeSession();
}
