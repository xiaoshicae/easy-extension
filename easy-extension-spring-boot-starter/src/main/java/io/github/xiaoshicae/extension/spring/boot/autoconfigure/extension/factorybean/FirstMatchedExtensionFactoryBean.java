package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.proxy.extpoint.MatchedExtensionDynamicProxyFactory;
import org.springframework.beans.factory.FactoryBean;

public class FirstMatchedExtensionFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> extensionPointClass;
    private MatchedExtensionDynamicProxyFactory<T> extensionProxyFactory;

    public FirstMatchedExtensionFactoryBean(Class<T> extensionPointClass) {
        this.extensionPointClass = extensionPointClass;
    }

    @Override
    public T getObject() throws Exception {
        return getExtensionProxyFactory().newFirstMatchedInstance();
    }

    @Override
    public Class<T> getObjectType() {
        return extensionPointClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public MatchedExtensionDynamicProxyFactory<T> getExtensionProxyFactory() {
        return extensionProxyFactory;
    }

    public void setExtensionProxyFactory(MatchedExtensionDynamicProxyFactory<T> extensionProxyFactory) {
        this.extensionProxyFactory = extensionProxyFactory;
    }
}
