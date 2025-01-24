package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.AllMatchedExtPointProxyFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private final AllMatchedExtPointProxyFactory<T> allMatchedExtPointProxyFactory;

    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.allMatchedExtPointProxyFactory = new AllMatchedExtPointProxyFactory<>(extensionPointClass, extensionFactory);
    }

    @Override
    public List<T> getObject() throws Exception {
        return allMatchedExtPointProxyFactory.getProxy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<T>> getObjectType() {
        return (Class<List<T>>) (Class<?>) List.class;
    }
}
