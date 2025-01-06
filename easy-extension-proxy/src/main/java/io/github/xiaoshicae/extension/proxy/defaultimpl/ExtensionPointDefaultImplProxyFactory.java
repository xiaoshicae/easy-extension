package io.github.xiaoshicae.extension.proxy.defaultimpl;

import io.github.xiaoshicae.extension.core.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.exception.ProxyParamException;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyDefinition;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyUtil;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExtensionPointDefaultImplProxyFactory<T> {

    @SuppressWarnings("unchecked")
    public IExtensionPointGroupDefaultImplementation<T> newExtensionPointDefaultImplProxy(Class<?> rawClass) throws ProxyException {
        if (Objects.isNull(rawClass)) {
            throw new ProxyParamException("class should not be null");
        }
        ExtensionPointDefaultImplementation annotation = AnnotationUtils.findAnnotation(rawClass, ExtensionPointDefaultImplementation.class);
        if (Objects.isNull(annotation)) {
            throw new ProxyParamException(String.format("extension point default implementation [%s] must annotated with @Ability", rawClass.getSimpleName()));
        }

        List<Class<?>> extensionPointClasses = Arrays.stream(rawClass.getInterfaces()).filter((c) -> c.isAnnotationPresent(ExtensionPoint.class)).toList();
        ExtensionPointDefaultImplementationProxy<T> proxy = new ExtensionPointDefaultImplementationProxy<>();
        proxy.setImplementsExtensions(extensionPointClasses);

        CGLibProxyDefinition<IExtensionPointGroupDefaultImplementation<?>> definition = new CGLibProxyDefinition<>();
        definition.setSuperClass(rawClass);
        definition.setInstance(proxy);
        definition.setImplInterfaces(IExtensionPointGroupDefaultImplementation.class);
        return (IExtensionPointGroupDefaultImplementation<T>) CGLibProxyUtil.newCGLibProxy(definition);
    }

    public static class ExtensionPointDefaultImplementationProxy<T> extends AbstractExtensionPointDefaultImplementation<T> {
        private List<Class<?>> implementsExtensions;

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return implementsExtensions;
        }

        public void setImplementsExtensions(List<Class<?>> implementsExtensions) {
            this.implementsExtensions = implementsExtensions;
        }
    }
}

