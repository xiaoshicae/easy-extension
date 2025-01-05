package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.proxy.ability.AbilityProxyFactory;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AbilityFactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Objects;

public class AbilityScanner extends ClassPathBeanDefinitionScanner {
    private final Class<? extends Annotation> annClass = Ability.class;
    private final Class<?> factoryBeanClass = AbilityFactoryBean.class;

    public AbilityScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(annClass));
    }

    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        AbilityProxyFactory<?> abilityProxyFactory = new AbilityProxyFactory<>();

        String beanClassName = Objects.requireNonNull(holder.getBeanDefinition().getBeanClassName());

        IAbility<?> abilityProxy;
        try {
            Class<?> beanClass = ClassUtils.forName(beanClassName, ClassUtils.getDefaultClassLoader());
            abilityProxy = abilityProxyFactory.newAbilityProxy(beanClass);
        } catch (ProxyException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(factoryBeanClass);
        builder.addConstructorArgValue(abilityProxy);
        registry.registerBeanDefinition(holder.getBeanName(), builder.getBeanDefinition());
    }
}
