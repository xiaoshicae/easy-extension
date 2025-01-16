package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Objects;

public class ExtensionPointScanner extends ClassPathBeanDefinitionScanner {
    public ExtensionPointScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        String extensionPointClassName = holder.getBeanDefinition().getBeanClassName();
        registerFirstMatchedExtensionFactoryBeanDefinition(extensionPointClassName, registry);
        registerAllMatchedExtensionBeanFactoryDefinition(extensionPointClassName, registry);
        registerExtensionPointHolderBeanDefinition(extensionPointClassName, registry);
    }

    private void registerFirstMatchedExtensionFactoryBeanDefinition(String extensionPointClassName, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FirstMatchedExtensionFactoryBean.class);
        builder.addConstructorArgValue(extensionPointClassName);
        String beanName = ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(extensionPointClassName);
        Objects.requireNonNull(registry).registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private void registerAllMatchedExtensionBeanFactoryDefinition(String extensionPointClassName, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AllMatchedExtensionFactoryBean.class);
        builder.addConstructorArgValue(extensionPointClassName);
        String beanName = ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(extensionPointClassName);
        Objects.requireNonNull(registry).registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private void registerExtensionPointHolderBeanDefinition(String extensionPointClassName, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionPointHolder.class);
        builder.addConstructorArgValue(extensionPointClassName);
        String beanName = ExtensionPointBeanNameGenerator.genExtensionClassHolderBeanName(extensionPointClassName);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
