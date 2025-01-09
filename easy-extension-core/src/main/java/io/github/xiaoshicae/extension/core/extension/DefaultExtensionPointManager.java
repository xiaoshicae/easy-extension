package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultExtensionPointManager implements IExtensionPointManager {
    private static final String keySeparator = "#";
    private final Map<String, Object> extensionInstances = new ConcurrentHashMap<>();

    @Override
    public <T> void registerExtensionPointImplementationInstance(Class<T> extensionPointClass, String name, T instance) throws RegisterException {
        if (Objects.isNull(extensionPointClass)) {
            throw new RegisterParamException("extension point class should not be null");
        }
        if (!extensionPointClass.isInterface()) {
            throw new RegisterParamException("extension point class should not be interface type");
        }
        if (Objects.isNull(name)) {
            throw new RegisterParamException("name should not be null");
        }
        if (Objects.isNull(instance)) {
            throw new RegisterParamException("instance should not be null");
        }

        String key = makeKey(extensionPointClass, name);
        if (extensionInstances.containsKey(key)) {
            throw new RegisterDuplicateException(String.format("extension point [%s] with name [%s] already registered", extensionPointClass.getName(), name));
        }
        extensionInstances.put(key, instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtensionPointImplementationInstance(Class<T> extensionPointClass, String name) throws QueryException {
        if (Objects.isNull(extensionPointClass)) {
            throw new QueryParamException("extension point class should not be null");
        }
        if (!extensionPointClass.isInterface()) {
            throw new QueryParamException("extension point class should be an interface type");
        }
        if (Objects.isNull(name)) {
            throw new QueryParamException("name should not be null");
        }
        Object instance = extensionInstances.get(makeKey(extensionPointClass, name));
        if (Objects.isNull(instance)) {
            throw new QueryNotFoundException(String.format("instance not found by extension point class [%s] + name [%s]", name, extensionPointClass.getSimpleName()));
        }
        return (T) instance;
    }

    private String makeKey(Class<?> extensionPointClass, String name) {
        return extensionPointClass.getName() + keySeparator + name;
    }
}
