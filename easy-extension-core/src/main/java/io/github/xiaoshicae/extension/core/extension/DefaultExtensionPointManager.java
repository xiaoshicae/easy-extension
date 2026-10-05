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
    // extension point class -> (instance name -> instance); nested maps avoid building a composite key per lookup
    private final Map<Class<?>, Map<String, Object>> extensionInstances = new ConcurrentHashMap<>();

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

        Object existing = extensionInstances
                .computeIfAbsent(extensionPointClass, k -> new ConcurrentHashMap<>())
                .putIfAbsent(name, instance);
        if (existing != null) {
            throw new RegisterDuplicateException(String.format("extension point [%s] with name [%s] already registered", extensionPointClass.getName(), name));
        }
    }

    @Override
    public <T> T getExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException {
        T instance = findExtensionPointImplementationInstance(extensionPointClass, name);
        if (instance == null) {
            throw new QueryNotFoundException(String.format("instance not found by extension point class [%s] + name [%s]", extensionPointClass.getSimpleName(), name));
        }
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T findExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException {
        if (extensionPointClass == null) {
            throw new QueryParamException("extension point class should not be null");
        }
        if (!extensionPointClass.isInterface()) {
            throw new QueryParamException("extension point class should be an interface type");
        }
        if (name == null) {
            throw new QueryParamException("name should not be null");
        }
        Map<String, Object> instances = extensionInstances.get(extensionPointClass);
        return instances == null ? null : (T) instances.get(name);
    }
}
