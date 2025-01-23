package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.common.IProxy;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtPointDefaultImplProxyFactory<T> {
    private final ExtensionPointGroupDefaultImplementationTemplate<T> tpl;

    public ExtPointDefaultImplProxyFactory(Object extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new ExtensionPointGroupDefaultImplementationTemplate<>(extImplInstance, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IExtensionPointGroupDefaultImplementation<T> getProxy() {
        Class<?>[] interfaces = new Class[tpl.implementExtensionPoints().size() + 1];
        interfaces[0] = IExtensionPointGroupDefaultImplementationProxy.class;
        for (int i = 0; i < tpl.implementExtensionPoints().size(); i++) {
            interfaces[i + 1] = tpl.implementExtensionPoints().get(i);
        }
        return (IExtensionPointGroupDefaultImplementation<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new ExtensionDefaultImplInvocationHandler<>(tpl, tpl.getInstance())
        );
    }

    public interface IExtensionPointGroupDefaultImplementationProxy<T> extends IExtensionPointGroupDefaultImplementation<T>, IProxy<Object> {
    }

    private static class ExtensionPointGroupDefaultImplementationTemplate<T> extends AbstractExtensionPointDefaultImplementation<T> implements IExtensionPointGroupDefaultImplementationProxy<T> {
        private final Object extImplInstance;
        private final List<Class<?>> implExtPoints;

        public ExtensionPointGroupDefaultImplementationTemplate(Object extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
            ValidateInstanceUtils.validateInstance(extImplInstance, implExtPoints);
            this.extImplInstance = extImplInstance;
            this.implExtPoints = implExtPoints;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return implExtPoints;
        }

        @Override
        public Object getInstance() {
            return extImplInstance;
        }
    }

    private static class ExtensionDefaultImplInvocationHandler<T> implements InvocationHandler {
        private final IExtensionPointGroupDefaultImplementation<T> defaultImplProxy;
        private final Object extDefaultImplInstance;
        private static final Set<Method> defaultImplProxyMethodCache = new HashSet<>(List.of(IExtensionPointGroupDefaultImplementationProxy.class.getMethods()));

        public ExtensionDefaultImplInvocationHandler(IExtensionPointGroupDefaultImplementation<T> defaultImplProxy, Object extDefaultImplInstance) {
            this.defaultImplProxy = defaultImplProxy;
            this.extDefaultImplInstance = extDefaultImplInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return defaultImplProxyMethodCache.contains(method) ? method.invoke(defaultImplProxy, args) : method.invoke(extDefaultImplInstance, args);
        }
    }
}
