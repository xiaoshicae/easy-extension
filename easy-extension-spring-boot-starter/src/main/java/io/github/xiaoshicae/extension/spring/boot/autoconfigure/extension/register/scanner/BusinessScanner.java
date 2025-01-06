package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.proxy.business.BusinessProxyFactory;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.BusinessFactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Objects;

public class BusinessScanner extends ClassPathBeanDefinitionScanner {
    private final Class<? extends Annotation> annClass = Business.class;
    private final Class<?> factoryBeanClass = BusinessFactoryBean.class;

    public BusinessScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(annClass));
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        BusinessProxyFactory<?> businessProxyFactory = new BusinessProxyFactory<>();

        String beanClassName = Objects.requireNonNull(holder.getBeanDefinition().getBeanClassName());

        IBusiness<?> businessProxy;
        try {
            Class<?> beanClass = ClassUtils.forName(beanClassName, ClassUtils.getDefaultClassLoader());
            businessProxy = businessProxyFactory.newBusinessProxy(beanClass);
        } catch (ProxyException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(factoryBeanClass);
        builder.addConstructorArgValue(businessProxy);
        registry.registerBeanDefinition(holder.getBeanName(), builder.getBeanDefinition());
    }
}
