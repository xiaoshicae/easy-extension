package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.defaultimpl.ExtensionPointDefaultImplProxyFactory;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.ExtensionDefaultImplFactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

public class ExtensionPointDefaultImplScanner extends ClassPathBeanDefinitionScanner {
    private final Class<? extends Annotation> annClass = ExtensionPointDefaultImplementation.class;
    private final Class<?> factoryBeanClass = ExtensionDefaultImplFactoryBean.class;

    public ExtensionPointDefaultImplScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(annClass));
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.size() > 1) {
            throw new IllegalArgumentException(String.format("@ExtensionPointDefaultImplementation more than one scanned in [%s]", String.join(", ", basePackages)));
        }

        return beanDefinitions;
    }

    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        ExtensionPointDefaultImplProxyFactory<?> factory = new ExtensionPointDefaultImplProxyFactory<>();

        String beanClassName = Objects.requireNonNull(holder.getBeanDefinition().getBeanClassName());

        IExtensionPointGroupDefaultImplementation<?> defaultImplementationProxy;
        try {
            Class<?> beanClass = ClassUtils.forName(beanClassName, ClassUtils.getDefaultClassLoader());
            defaultImplementationProxy = factory.newExtensionPointDefaultImplProxy(beanClass);
        } catch (ProxyException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(factoryBeanClass);
        builder.addConstructorArgValue(defaultImplementationProxy);
        registry.registerBeanDefinition(holder.getBeanName(), builder.getBeanDefinition());
    }
}
