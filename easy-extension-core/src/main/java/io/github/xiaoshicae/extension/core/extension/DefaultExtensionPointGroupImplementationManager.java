package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class DefaultExtensionPointGroupImplementationManager<T> implements IExtensionPointGroupImplementationManager<T> {
    private final IExtensionPointManager extensionPointManager = new DefaultExtensionPointManager();

    @Override
    public void registerExtensionPointImplementationInstance(IExtensionPointGroupImplementation<T> instance) throws RegisterException {
        if (instance == null) {
            throw new RegisterParamException("instance should not be null");
        }
        if (instance.code() == null) {
            throw new RegisterParamException("instance code should not be null");
        }
        List<Class<?>> extensionPoints = instance.implementExtensionPoints();
        // all or nothing: look at every extension point first, so that a failure leaves nothing registered
        Set<Class<?>> seen = new HashSet<>();
        for (Class<?> clazz : extensionPoints) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("instance implement extension point class [%s] invalid, class should be an interface type", clazz.getName()));
            }
            if (!clazz.isInstance(instance)) {
                throw new RegisterParamException(String.format("instance not implement extension point class [%s]", clazz.getName()));
            }
            if (!seen.add(clazz) || isRegistered(clazz, instance.code())) {
                throw new RegisterDuplicateException(String.format("extension point [%s] with name [%s] already registered", clazz.getName(), instance.code()));
            }
        }
        for (Class<?> clazz : extensionPoints) {
            register(extensionPointManager, clazz, instance.code(), instance);
        }
    }

    private boolean isRegistered(Class<?> extensionPoint, String code) {
        try {
            return extensionPointManager.findExtensionPointImplementationInstance(extensionPoint, code) != null;
        } catch (QueryException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(IExtensionPointManager manager, Class<T> extensionPoint, String code, Object instance) throws RegisterException {
        manager.registerExtensionPointImplementationInstance(extensionPoint, code, (T) instance);
    }

    @Override
    public <E> E getExtensionPointImplementationInstance(Class<E> extensionPoint, String code) throws QueryException {
        return extensionPointManager.getExtensionPointImplementationInstance(extensionPoint, code);
    }

    @Override
    public <E> E findExtensionPointImplementationInstance(Class<E> extensionPoint, String code) throws QueryException {
        return extensionPointManager.findExtensionPointImplementationInstance(extensionPoint, code);
    }
}
