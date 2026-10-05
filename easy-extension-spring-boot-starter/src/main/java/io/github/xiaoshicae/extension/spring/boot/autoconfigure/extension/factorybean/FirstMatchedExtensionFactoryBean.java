package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionProxies;
import org.springframework.beans.factory.FactoryBean;

/**
 * The injectable proxy of an extension point: every call goes to the first matching implementation of the
 * resolution bound to the calling thread.
 */
public class FirstMatchedExtensionFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> extensionPointClass;
    private final ExtensionContext<?> context;

    public FirstMatchedExtensionFactoryBean(Class<T> extensionPointClass, ExtensionContext<?> context) {
        this.extensionPointClass = extensionPointClass;
        this.context = context;
    }

    @Override
    public T getObject() {
        return ExtensionProxies.proxy(context, extensionPointClass);
    }

    @Override
    public Class<T> getObjectType() {
        return extensionPointClass;
    }
}
