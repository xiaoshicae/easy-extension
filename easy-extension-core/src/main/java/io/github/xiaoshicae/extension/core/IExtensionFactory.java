package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;

import java.util.List;

public interface IExtensionFactory {

    /**
     * Get matched extension implementation instance with max priority.
     *
     * @param extensionPointType extension point type
     * @return matched extension implementation instance with max priority
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getFirstMatchedExtension(Class<T> extensionPointType) throws QueryException;

    /**
     * Get all matched extension instance.
     *
     * @param extensionPointType extension point type
     * @return all matched extension instance.
     * @throws QueryNotFoundException if instance not found
     */
    <T> List<T> getAllMatchedExtension(Class<T> extensionPointType) throws QueryException;

    /**
     * Get scoped matched extension implementation instance with max priority.
     *
     * @param scope namespace of session
     * @param extensionPointType extension point type
     * @return scoped all matched extension instance.
     * @throws QueryNotFoundException if instance not found
     */
    <T> T getScopedFirstMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException;

    /**
     * Get scoped all matched extension instance.
     *
     * @param extensionPointType extension point type
     * @return scoped all matched extension instance.
     * @throws QueryNotFoundException if instance not found
     */
    <T> List<T> getScopedAllMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException;
}
