package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.proxy.AllMatchedExtPointProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private final Class<T> extensionPointClass;
    private final AllMatchedExtPointProxyFactory<T> allMatchedExtPointProxyFactory;

    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.allMatchedExtPointProxyFactory = new AllMatchedExtPointProxyFactory<>(extensionPointClass, extensionFactory);
    }

    @Override
    public List<T> getObject() throws Exception {
        return allMatchedExtPointProxyFactory.getProxy();
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
}
