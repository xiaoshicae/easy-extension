package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

/**
 * Because there are different implementations of the extension point,
 * and an instance may implement different extension point,
 * this manager designed base on {@code IExtensionPointManager}, manage many to many situation.
 *
 * @param <T> type define for {@code IExtensionPointGroupImplementation<T>}
 */
public interface IExtensionPointGroupImplementationManager<T> {

    /**
     * Register an instance that implements a group of extension point,
     * manager will loop extension point and register extension point class + name  to {@code IExtensionPointManager}
     *
     * @param instance instance that implements a group extension point
     * @throws RegisterParamException     if {@code instance} is null
     * @throws RegisterDuplicateException if extension point class + name already register to {@code IExtensionPointManager}
     */
    void registerExtensionPointImplementationInstance(IExtensionPointGroupImplementation<T> instance, String name) throws RegisterException;

    /**
     * Get an instance from the manager by extension point class + name.
     *
     * @param extensionPoint extension point class
     * @param name           name of instance
     * @return instance that implement the given extension point class
     * @throws QueryParamException    if {@code extensionPoint} is null, {@code extensionPoint} is not an interface type or {@code name} is null
     * @throws QueryNotFoundException if instance not found
     */
    <E> E getExtensionPointImplementationInstance(Class<E> extensionPoint, String name) throws QueryException;
}
