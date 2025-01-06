package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.proxy.extpoint.MatchedExtensionDynamicProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private final Class<T> extensionPointClass;
    private MatchedExtensionDynamicProxyFactory<T> extensionProxyFactory;

    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass) {
        this.extensionPointClass = extensionPointClass;
    }

    @Override
    public List<T> getObject() throws Exception {
        return getExtensionProxyFactory().newAllMatchedInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
        return (Class<T>) List.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<T> getExtensionPointClass() {
        return extensionPointClass;
    }

    public MatchedExtensionDynamicProxyFactory<T> getExtensionProxyFactory() {
        return extensionProxyFactory;
    }

    public void setExtensionProxyFactory(MatchedExtensionDynamicProxyFactory<T> extensionProxyFactory) {
        this.extensionProxyFactory = extensionProxyFactory;
    }
}
