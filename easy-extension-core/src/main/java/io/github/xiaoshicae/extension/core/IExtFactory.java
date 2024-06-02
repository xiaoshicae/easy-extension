package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import java.util.List;

public interface IExtFactory {
    /**
     * get matched extension instance that has max priority
     *
     * @param <E>           extension type
     * @param extensionType extension.class
     * @return instance that implements extension with max priority
     * @throws ExtensionException extension not found
     */
    <E> E getFirstMatchedExtension(Class<E> extensionType) throws ExtensionException;

    /**
     * get all matched extension instance
     *
     * @param <E> extension type
     * @param extensionType extension.class
     * @return all instance that implements extension
     * @throws ExtensionException extension not found
     */
    <E> List<E> getAllMatchedExtension(Class<E> extensionType) throws ExtensionException;
}
