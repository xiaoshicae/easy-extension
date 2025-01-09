package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

/**
 * Since an extension point may have multiple implementations,
 * and each instance may also implement multiple extension points,
 * the design goal of the manager is to manage the above many-to-many relationship scenario.
 * <br>
 * Instance implement multiple extension points,
 * and each extension point will be managed by {@link  IExtensionPointManager}
 *
 * @param <T> matcher param class
 */
public interface IExtensionPointGroupImplementationManager<T> {

    /**
     * Register an instance that implements a group of extension point.
     * <br>
     * Manager will loop extension points that the instance implements,
     * and each extension point class will be managed by {@link IExtensionPointManager}
     *
     * @param instance instance that implements a group extension point
     * @throws RegisterParamException     if {@code instance} is null
     * @throws RegisterDuplicateException if {@code instance} already register
     */
    void registerExtensionPointImplementationInstance(IExtensionPointGroupImplementation<T> instance) throws RegisterException;

    /**
     * Get an instance from the manager by extension point class + instance code.
     *
     * @param extensionPointClass extension point class
     * @param code                code of instance
     * @return instance that implement {@code extensionPointClass}
     * @throws QueryParamException    if {@code extensionPointClass} is null, {@code extensionPointClass} is not an interface
     *                                or {@code code} is null
     * @throws QueryNotFoundException if instance not found
     */
    <E> E getExtensionPointImplementationInstance(Class<E> extensionPointClass, String code) throws QueryException;
}
