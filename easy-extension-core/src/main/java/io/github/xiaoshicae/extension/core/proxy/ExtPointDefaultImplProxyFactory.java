package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.lang.reflect.Proxy;
import java.util.List;

public class ExtPointDefaultImplProxyFactory<T> {
    private final ExtensionPointGroupDefaultImplementationTemplate<T> tpl;

    public ExtPointDefaultImplProxyFactory(Object extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new ExtensionPointGroupDefaultImplementationTemplate<>(extImplInstance, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IExtensionPointGroupDefaultImplementation<T> getProxy() {
        Class<?>[] interfaces = ProxyUtils.buildInterfaces(IExtensionPointGroupDefaultImplementationProxy.class, tpl.implementExtensionPoints());
        return (IExtensionPointGroupDefaultImplementation<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new DelegatingInvocationHandler(tpl, tpl.getInstance(), IExtensionPointGroupDefaultImplementationProxy.class)
        );
    }

    public interface IExtensionPointGroupDefaultImplementationProxy<T> extends IExtensionPointGroupDefaultImplementation<T>, IProxy<Object> {
    }

    private static class ExtensionPointGroupDefaultImplementationTemplate<T> extends AbstractExtensionPointDefaultImplementation<T> implements IExtensionPointGroupDefaultImplementationProxy<T> {
        private final Object extImplInstance;
        private final List<Class<?>> implExtPoints;

        public ExtensionPointGroupDefaultImplementationTemplate(Object extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
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

}
