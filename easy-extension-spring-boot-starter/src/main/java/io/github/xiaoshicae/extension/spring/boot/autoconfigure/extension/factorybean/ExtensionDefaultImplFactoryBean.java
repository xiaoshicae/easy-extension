package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import org.springframework.beans.factory.FactoryBean;

public class ExtensionDefaultImplFactoryBean<T> implements FactoryBean<IExtensionPointGroupDefaultImplementation<T>> {
    private final IExtensionPointGroupDefaultImplementation<T> instance;

    public ExtensionDefaultImplFactoryBean(IExtensionPointGroupDefaultImplementation<T> instance) {
        this.instance = instance;
    }

    @Override
    public IExtensionPointGroupDefaultImplementation<T> getObject() throws Exception {
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
        return (Class<T>) IExtensionPointGroupDefaultImplementation.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
