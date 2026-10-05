package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionProxies;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

/**
 * The injectable list of all matching implementations of an extension point for the resolution bound to the calling thread.
 */
public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private final Class<T> extensionPointClass;
    private final ExtensionContext<?> context;

    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass, ExtensionContext<?> context) {
        this.extensionPointClass = extensionPointClass;
        this.context = context;
    }

    @Override
    public List<T> getObject() {
        return ExtensionProxies.proxyAll(context, extensionPointClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<T>> getObjectType() {
        return (Class<List<T>>) (Class<?>) List.class;
    }
}
