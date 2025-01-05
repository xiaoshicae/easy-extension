package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultExtensionPointManager implements IExtensionPointManager {
    private static final String keySeparator = "#";
    private final Map<String, Object> extensionInstances = new HashMap<>();

    @Override
    public <T> void registerExtensionPointImplementationInstance(Class<T> extensionPoint, String name, T instance) throws RegisterException {
        if (Objects.isNull(extensionPoint)) {
            throw new RegisterParamException("extension point class should not be null");
        }
        if (!extensionPoint.isInterface()) {
            throw new RegisterParamException("extension point class should not be interface type");
        }
        if (Objects.isNull(name)) {
            throw new RegisterParamException("name should not be null");
        }
        if (Objects.isNull(instance)) {
            throw new RegisterParamException("instance should not be null");
        }

        String key = makeKey(extensionPoint, name);
        if (extensionInstances.containsKey(key)) {
            throw new RegisterDuplicateException(String.format("extension point [%s] with name [%s] already registered", extensionPoint.getName(), name));
        }
        extensionInstances.put(key, instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtensionPointImplementationInstance(Class<T> extensionPoint, String name) throws QueryException {
        if (Objects.isNull(extensionPoint)) {
            throw new QueryParamException("extension point class should not be null");
        }
        if (!extensionPoint.isInterface()) {
            throw new QueryParamException("extension point class should be an interface type");
        }
        if (Objects.isNull(name)) {
            throw new QueryParamException("name should not be null");
        }
        Object instance = extensionInstances.get(makeKey(extensionPoint, name));
        if (Objects.isNull(instance)) {
            throw new QueryNotFoundException(String.format("instance with name [%s] of extension point [%s] not found", name, extensionPoint.getSimpleName()));
        }
        return (T) instance;
    }

    private String makeKey(Class<?> extensionType, String name) {
        return extensionType.getName() + keySeparator + name;
    }
}
