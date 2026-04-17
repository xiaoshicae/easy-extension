package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;

import java.util.List;

public interface IExtensionFactory {

    /**
     * Get matched extension implementation instance with max priority
     * from the default session scope.
     *
     * @param extensionPointType extension point type
     * @return matched extension implementation instance with max priority
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getFirstMatchedExtension(Class<T> extensionPointType) throws QueryException;

    /**
     * Get all matched extension instance from the default session scope.
     *
     * @param extensionPointType extension point type
     * @return all matched extension instance.
     * @throws QueryNotFoundException if instance not found
     */
    <T> List<T> getAllMatchedExtension(Class<T> extensionPointType) throws QueryException;

    /**
     * Scope-aware overload of {@link #getFirstMatchedExtension(Class)}.
     *
     * @param scope              namespace of session
     * @param extensionPointType extension point type
     * @return matched extension implementation instance with max priority
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getFirstMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException;

    /**
     * Scope-aware overload of {@link #getAllMatchedExtension(Class)}.
     *
     * @param scope              namespace of session
     * @param extensionPointType extension point type
     * @return all matched extension instances
     * @throws QueryNotFoundException if instance not found
     */
    <T> List<T> getAllMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException;

    /**
     * @deprecated Use {@link #getFirstMatchedExtension(String, Class)} — the
     *             scoped/non-scoped pair has been collapsed into a single
     *             overload. This method will be removed in a future release.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default <T> T getScopedFirstMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException {
        return getFirstMatchedExtension(scope, extensionPointType);
    }

    /**
     * @deprecated Use {@link #getAllMatchedExtension(String, Class)}.
     */
    @Deprecated(since = "3.4", forRemoval = true)
    default <T> List<T> getScopedAllMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException {
        return getAllMatchedExtension(scope, extensionPointType);
    }
}
