package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultExtensionPointManager implements IExtensionPointManager {
    private static final String KEY_SEPARATOR = "#";
    private final Map<String, Object> extensionInstances = new ConcurrentHashMap<>();

    @Override
    public <T> void registerExtensionPointImplementationInstance(Class<T> extensionPointClass, String name, T instance) throws RegisterException {
        if (extensionPointClass == null) {
            throw new RegisterParamException("extension point class should not be null");
        }
        if (!extensionPointClass.isInterface()) {
            throw new RegisterParamException("extension point class should be an interface type");
        }
        if (name == null) {
            throw new RegisterParamException("name should not be null");
        }
        if (instance == null) {
            throw new RegisterParamException("instance should not be null");
        }

        String key = makeKey(extensionPointClass, name);
        Object existing = extensionInstances.putIfAbsent(key, instance);
        if (existing != null) {
            throw new RegisterDuplicateException(String.format("extension point [%s] with name [%s] already registered", extensionPointClass.getName(), name));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException {
        if (extensionPointClass == null) {
            throw new QueryParamException("extension point class should not be null");
        }
        if (!extensionPointClass.isInterface()) {
            throw new QueryParamException("extension point class should be an interface type");
        }
        if (name == null) {
            throw new QueryParamException("name should not be null");
        }
        Object instance = extensionInstances.get(makeKey(extensionPointClass, name));
        if (instance == null) {
            throw new QueryNotFoundException(String.format("instance not found by extension point class [%s] + name [%s]", extensionPointClass.getSimpleName(), name));
        }
        return (T) instance;
    }

    private String makeKey(Class<?> extensionPointClass, String name) {
        return extensionPointClass.getName() + KEY_SEPARATOR + name;
    }
}
