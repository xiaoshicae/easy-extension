package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.business.IBusiness;
import org.springframework.beans.factory.FactoryBean;

public class BusinessFactoryBean<T> implements FactoryBean<IBusiness<T>> {
    private final IBusiness<T> instance;

    public BusinessFactoryBean(IBusiness<T> instance) {
        this.instance = instance;
    }

    @Override
    public IBusiness<T> getObject() throws Exception {
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
        return (Class<T>) IBusiness.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
