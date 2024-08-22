package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.ExtensionException;

public interface IExtContext<T> extends IExtFactory, IExtRegister<T>, ISessionManager<T> {

    /**
     * Check registered business ability is valid
     *
     * @throws ExtensionException when context is invalid
     */
    void validateContext() throws ExtensionException;
}
