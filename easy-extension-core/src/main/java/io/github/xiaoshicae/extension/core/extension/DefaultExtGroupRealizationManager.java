package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;
import io.github.xiaoshicae.extension.core.exception.ExtensionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.ExtensionTypeException;

import java.util.HashMap;
import java.util.Map;

public class DefaultExtGroupRealizationManager implements IExtGroupRealizationManager {
    private static final String keySeparator = "#";

    private final Map<String, Object> extensionInstances = new HashMap<>();

    @Override
    public <T extends IExtGroupRealization<?>> void registerExtGroupRealization(T instance, String name) throws ExtensionException {
        if (name == null) {
            throw new ExtensionException("name can not be null");
        }

        if (instance == null) {
            throw new ExtensionTypeException("instance can not be null");
        }

        for (Class<?> clazz : instance.getClass().getInterfaces()) {
            if (clazz.isAnnotationPresent(ExtensionPoint.class)) {
                if (extensionInstances.containsKey(makeKey(clazz, name))) {
                    throw new ExtensionTypeException("instance " + clazz.getName() + " with name " + name + " already registered");
                }

                extensionInstances.put(makeKey(clazz, name), instance);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getExtGroupRealization(Class<T> extensionType, String name) throws ExtensionException {
        if (name == null) {
            throw new ExtensionException("name can not be null");
        }
        if (extensionType == null) {
            throw new ExtensionTypeException("extensionType can not be null");
        }
        if (!extensionType.isInterface()) {
            throw new ExtensionTypeException("extensionType must be an interface");
        }
        if (!extensionType.isAnnotationPresent(ExtensionPoint.class)) {
            throw new ExtensionTypeException("extensionType must be annotated with @ExtensionPoint");
        }

        Object instance = extensionInstances.get(makeKey(extensionType, name));
        if (instance == null) {
            throw new ExtensionNotFoundException("extension " + name + " not found");
        }

        if (!extensionType.isInstance(instance)) {
            throw new ExtensionTypeException("extension " + name + " is not an instance of " + extensionType.getName());
        }

        return (T) instance;
    }

    private String makeKey(Class<?> extensionType, String name) {
        return extensionType.getName() + keySeparator + name;
    }
}
