package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import org.springframework.beans.factory.FactoryBean;

public class AbilityFactoryBean<T> implements FactoryBean<IAbility<T>> {
    private final IAbility<T> instance;

    public AbilityFactoryBean(IAbility<T> instance) {
        this.instance = instance;
    }

    @Override
    public IAbility<T> getObject() throws Exception {
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
        return (Class<T>) IAbility.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
