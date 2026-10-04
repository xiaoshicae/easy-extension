package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.lang.reflect.Proxy;
import java.util.List;

public class ExtPointDefaultImplProxyFactory<T> {
    private final ExtensionPointGroupDefaultImplementationTemplate<T> tpl;

    public ExtPointDefaultImplProxyFactory(Object extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this(extImplInstance, Utils.classOf(extImplInstance), implExtPoints);
    }

    /**
     * @param extImplInstance the default implementation
     * @param targetClass     the class that carries the metadata of the implementation; differs from the class of
     *                        {@code extImplInstance} when that is a proxy created by a container
     * @param implExtPoints   the extension points the implementation implements
     * @throws ProxyException if {@code targetClass} is null, an extension point is not a public interface, or the
     *                        instance does not implement it
     * @since 3.4
     */
    public ExtPointDefaultImplProxyFactory(Object extImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new ExtensionPointGroupDefaultImplementationTemplate<>(extImplInstance, targetClass, implExtPoints);
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
        private final Class<?> targetClass;
        private final List<Class<?>> implExtPoints;

        public ExtensionPointGroupDefaultImplementationTemplate(Object extImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
            Utils.validateNoClash("a default implementation", IExtensionPointGroupDefaultImplementationProxy.class, implExtPoints);
            Utils.validateTargetClass(targetClass);
            this.extImplInstance = extImplInstance;
            this.targetClass = targetClass;
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

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }
    }

}
