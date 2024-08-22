package io.github.xiaoshicae.extension.core.extension;


import io.github.xiaoshicae.extension.core.exception.ExtensionException;

public interface IExtGroupRealizationManager {

    /**
     * register an instance with the manager
     *
     * @param instance instance that implements a group of extension point
     * @param name     name of instance
     * @throws ExtensionException name is null or extension type invalid(ExtensionTypeException)
     */
    <T extends IExtGroupRealization<?>> void registerExtGroupRealization(T instance, String name) throws ExtensionException;

    /**
     * get instance from the manager
     *
     * @param <T>           extension.class
     * @param extensionType extension type
     * @param name          name of instance
     * @return              instance that implement the given extension
     * @throws ExtensionException  name is null, extension type invalid(ExtensionTypeException) or extension not found(ExtensionNotFoundException)
     */
    <T> T getExtGroupRealization(Class<T> extensionType, String name) throws ExtensionException;
}
