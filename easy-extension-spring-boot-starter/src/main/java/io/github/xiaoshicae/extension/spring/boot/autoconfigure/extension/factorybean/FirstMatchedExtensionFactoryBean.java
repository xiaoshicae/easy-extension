package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.ExtensionProxyFactory;
import org.springframework.beans.factory.FactoryBean;

public class FirstMatchedExtensionFactoryBean<T> implements FactoryBean<T> {

    private Class<T> extensionInterface;

    private ExtensionProxyFactory<T> extensionProxyFactory;

    public FirstMatchedExtensionFactoryBean(Class<T> extensionInterface) {
        this.extensionInterface = extensionInterface;
    }

    @Override
    public T getObject() throws Exception {
        return getExtensionProxyFactory().newFirstMatchedInstance();
    }

    @Override
    public Class<T> getObjectType() {
        return this.extensionInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<T> getExtensionInterface() {
        return extensionInterface;
    }

    public void setExtensionInterface(Class<T> extensionInterface) {
        this.extensionInterface = extensionInterface;
    }

    public ExtensionProxyFactory<T> getExtensionProxyFactory() {
        return extensionProxyFactory;
    }

    public void setExtensionProxyFactory(ExtensionProxyFactory<T> extensionProxyFactory) {
        this.extensionProxyFactory = extensionProxyFactory;
    }
}
