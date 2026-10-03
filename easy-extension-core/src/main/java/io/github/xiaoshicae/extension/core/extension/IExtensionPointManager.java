package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

/**
 * Since an extension point may have multiple implementations,
 * and the manager is designed to manage the mapping relationship
 * between the extension point type + instance's unique name and the instance.
 *
 * <p>
 * An instance may implement multiple extension points, and the manager is used to manage one of them.
 * If you want to understand the management of all the extension points of an instance,
 * you can refer to {@link  IExtensionPointGroupImplementationManager}
 * </p>
 */
public interface IExtensionPointManager {

    /**
     * Register an instance with the manager
     * through the unique name of the extension point and the type of the extension point.
     * <br>Note: {@code extensionPointType} + {@code name} must be unique in the manager.
     *
     * @param <T>                 extension point type
     * @param extensionPointClass extension point type
     * @param name                name of instance
     * @param instance            instance that implement {@code extensionPointClass}
     * @throws RegisterParamException     if {@code extensionPointClass} is null,
     *                                    {@code extensionPointClass} is not an interface,
     *                                    {@code name} is null
     *                                    or {@code instance} is null
     * @throws RegisterDuplicateException if ({@code extensionPointClass} + {@code name}) already register
     */
    <T> void registerExtensionPointImplementationInstance(Class<T> extensionPointClass, String name, T instance) throws RegisterException;

    /**
     * Get an instance from the manager by {@code extensionPointClass} + {@code name}.
     *
     * @param extensionPointClass extension point type
     * @param name                name of instance
     * @return instance that implement {@code extensionPointClass}
     * @throws QueryParamException    if {@code extensionPointClass} is null, {@code extensionPointClass} is not an interface
     *                                or {@code name} is null
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException;

    /**
     * Like {@link #getExtensionPointImplementationInstance(Class, String)}, but answers {@code null} instead of
     * throwing {@link QueryNotFoundException} when there is no such instance.
     * <p>
     * A miss is the normal case on the lookup hot path (a business implements only some of the extension points,
     * the rest is served further down the chain), and creating an exception for each of them would dominate the
     * cost of a lookup. The default implementation delegates to
     * {@link #getExtensionPointImplementationInstance(Class, String)}, so existing implementations keep working;
     * override it with a lookup that does not throw.
     * </p>
     *
     * @return instance that implement {@code extensionPointClass}, or {@code null} if not found
     * @throws QueryParamException if {@code extensionPointClass} is null, {@code extensionPointClass} is not an
     *                             interface or {@code name} is null
     * @since 4.0
     */
    default <T> T findExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException {
        try {
            return getExtensionPointImplementationInstance(extensionPointClass, name);
        } catch (QueryNotFoundException e) {
            return null;
        }
    }
}
