package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.proxy.FirstMatchedExtPointProxyFactory;
import org.springframework.beans.factory.FactoryBean;

public class FirstMatchedExtensionFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> extensionPointClass;
    private final FirstMatchedExtPointProxyFactory<T> firstMatchedExtPointProxyFactory;

    public FirstMatchedExtensionFactoryBean(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.firstMatchedExtPointProxyFactory = new FirstMatchedExtPointProxyFactory<>(extensionPointClass, extensionFactory);
    }

    @Override
    public T getObject() throws Exception {
        return firstMatchedExtPointProxyFactory.getProxy();
    }

    @Override
    public Class<T> getObjectType() {
        return extensionPointClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
