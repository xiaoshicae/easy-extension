package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.proxy.IProxy;



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
        Object implementation = implementationOf(instance);
        for (Class<?> clazz : instance.implementExtensionPoints()) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("instance implement extension point class [%s] invalid, class should be an interface type", clazz.getName()));
            }
            if (!clazz.isInstance(implementation)) {
                throw new RegisterParamException(String.format("instance not implement extension point class [%s]", clazz.getName()));
            }
            register(extensionPointManager, clazz, instance.code(), implementation);
        }
    }

    /**
     * The object that actually answers extension point calls. For an annotation-based ability/business
     * the registered group is a framework descriptor proxy wrapping the user's object; register the user's
     * object itself so calls reach it directly instead of going through the descriptor proxy.
     */
    private static Object implementationOf(IExtensionPointGroupImplementation<?> instance) {
        if (instance instanceof IProxy<?> proxy) {
            Object real = proxy.getInstance();
            if (real != null && instance.implementExtensionPoints().stream().allMatch(c -> c.isInstance(real))) {
                return real;
            }
        }
        return instance;
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
