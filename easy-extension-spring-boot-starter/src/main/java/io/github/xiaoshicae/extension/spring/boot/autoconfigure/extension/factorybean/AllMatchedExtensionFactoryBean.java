package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.ExtensionProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private Class<T> extensionInterface;

    private ExtensionProxyFactory<T> extensionProxyFactory;

    public AllMatchedExtensionFactoryBean(Class<T> extensionInterface) {
        this.extensionInterface = extensionInterface;
    }

    @Override
    public List<T> getObject() throws Exception {
        return getExtensionProxyFactory().newAllMatchedInstance();
    }

    @Override
    public Class<?> getObjectType() {
        return List.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<?> getExtensionInterface() {
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
