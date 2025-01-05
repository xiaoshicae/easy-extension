package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

/**
 * Because there are different implementations of the extension point,
 * this manager is primarily designed to manage different implementations
 * based on their respective names.
 */
public interface IExtensionPointManager {

    /**
     * Register an instance with name that implements the given extension point,
     * note that in manager {@code extensionPoint} + {@code name} should be uniq.
     *
     * @param <T>            extension point class type
     * @param extensionPoint extension point class
     * @param name           name of instance
     * @param instance       instance that implements the given extension point
     * @throws RegisterParamException     if {@code extensionPoint} is null,
     *                                    {@code extensionPoint} is not an interface,
     *                                    {@code name} is null
     *                                    or {@code instance} is null
     * @throws RegisterDuplicateException if ({@code extensionPoint} + {@code name}) already register
     */
    <T> void registerExtensionPointImplementationInstance(Class<T> extensionPoint, String name, T instance) throws RegisterException;

    /**
     * Get an instance from the manager by {@code extensionPoint} + {@code name}.
     *
     * @param extensionPoint extension point class
     * @param name           name of instance
     * @return instance that implement the given extension point class
     * @throws QueryParamException    if {@code extensionPoint} is null, {@code extensionPoint} is not an interface type or {@code name} is null
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getExtensionPointImplementationInstance(Class<T> extensionPoint, String name) throws QueryException;
}
