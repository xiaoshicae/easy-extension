package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.Objects;

public class DefaultExtensionPointGroupImplementationManager<T> implements IExtensionPointGroupImplementationManager<T> {
    private final IExtensionPointManager extensionPointManager = new DefaultExtensionPointManager();

    @Override
    public void registerExtensionPointImplementationInstance(IExtensionPointGroupImplementation<T> instance, String name) throws RegisterException {
        if (Objects.isNull(instance)) {
            throw new RegisterParamException("instance should not be null");
        }
        if (Objects.isNull(name)) {
            throw new RegisterParamException("name should not be null");
        }
        for (Class<?> clazz : instance.implementExtensionPoints()) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("instance implement extension point class [%s] invalid, class should be an interface type", clazz.getName()));
            }
            if (!clazz.isInstance(instance)) {
                throw new RegisterParamException(String.format("instance not implement extension point class [%s]", clazz.getName()));
            }
            register(extensionPointManager, clazz, name, instance);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(IExtensionPointManager manager, Class<T> extensionPoint, String name, Object instance) throws RegisterException {
        manager.registerExtensionPointImplementationInstance(extensionPoint, name, (T) instance);
    }

    @Override
    public <E> E getExtensionPointImplementationInstance(Class<E> extensionPoint, String name) throws QueryException {
        return extensionPointManager.getExtensionPointImplementationInstance(extensionPoint, name);
    }
}
