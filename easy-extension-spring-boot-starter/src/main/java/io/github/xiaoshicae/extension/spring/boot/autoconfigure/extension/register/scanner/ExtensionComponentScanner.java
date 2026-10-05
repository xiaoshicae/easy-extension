package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * Scans the packages once for everything the framework needs from the classpath:
 * <ul>
 *   <li>{@code @ExtensionPoint} interfaces: registered as an {@link ExtensionPointHolder} plus the two injectable
 *   proxies (first matched, all matched);</li>
 *   <li>{@code @Ability}, {@code @Business} and {@code @DefaultImplementation} classes: registered as ordinary beans,
 *   so they can use dependency injection like any other component.</li>
 * </ul>
 */
public class ExtensionComponentScanner extends ClassPathBeanDefinitionScanner {

    public ExtensionComponentScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
        addIncludeFilter(new AnnotationTypeFilter(Ability.class));
        addIncludeFilter(new AnnotationTypeFilter(Business.class));
        addIncludeFilter(new AnnotationTypeFilter(DefaultImplementation.class));
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        if (metadata.isInterface()) {
            // extension points are interfaces, which the default check rejects
            return metadata.isIndependent() && metadata.isAnnotated(ExtensionPoint.class.getName());
        }
        // everything else must be a concrete class carrying one of the provider annotations
        // (an @ExtensionPoint on a class is not an extension point)
        return super.isCandidateComponent(beanDefinition) && isProvider(metadata);
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        AnnotationMetadata metadata = ((AnnotatedBeanDefinition) holder.getBeanDefinition()).getMetadata();
        if (metadata.isInterface()) {
            registerExtensionPoint(holder.getBeanDefinition().getBeanClassName(), registry);
        } else {
            super.registerBeanDefinition(holder, registry);
        }
    }

    private static boolean isProvider(AnnotationMetadata metadata) {
        return metadata.isAnnotated(Ability.class.getName())
                || metadata.isAnnotated(Business.class.getName())
                || metadata.isAnnotated(DefaultImplementation.class.getName());
    }

    private void registerExtensionPoint(String extensionPointClassName, BeanDefinitionRegistry registry) {
        ClassLoader classLoader = getResourceLoader() != null ? getResourceLoader().getClassLoader() : null;
        Class<?> extensionPointClass = ClassUtils.resolveClassName(extensionPointClassName, classLoader);

        // first matched: primary, so that plain by-type injection of the extension point also gets the proxy
        BeanDefinitionBuilder first = BeanDefinitionBuilder.genericBeanDefinition(FirstMatchedExtensionFactoryBean.class);
        first.addConstructorArgValue(extensionPointClassName);
        AbstractBeanDefinition firstDefinition = first.getBeanDefinition();
        firstDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, extensionPointClass);
        firstDefinition.setPrimary(true);
        registerIfAbsent(registry, ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(extensionPointClassName), firstDefinition);

        BeanDefinitionBuilder all = BeanDefinitionBuilder.genericBeanDefinition(AllMatchedExtensionFactoryBean.class);
        all.addConstructorArgValue(extensionPointClassName);
        AbstractBeanDefinition allDefinition = all.getBeanDefinition();
        allDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ResolvableType.forClassWithGenerics(List.class, extensionPointClass));
        registerIfAbsent(registry, ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(extensionPointClassName), allDefinition);

        BeanDefinitionBuilder holder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionPointHolder.class);
        holder.addConstructorArgValue(extensionPointClassName);
        registerIfAbsent(registry, ExtensionPointBeanNameGenerator.genExtensionClassHolderBeanName(extensionPointClassName), holder.getBeanDefinition());
    }

    // overlapping scan packages must not register the same extension point twice
    private static void registerIfAbsent(BeanDefinitionRegistry registry, String beanName, AbstractBeanDefinition definition) {
        if (!registry.containsBeanDefinition(beanName)) {
            registry.registerBeanDefinition(beanName, definition);
        }
    }
}
