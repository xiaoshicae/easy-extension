package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Objects;

public class ExtensionPointScanner extends ClassPathBeanDefinitionScanner {
    public ExtensionPointScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
    }

    /**
     * Whether the extension point is yet to be registered.
     * <p>
     * The name the scanner gives the interface (its short name, "greeter" for {@code Greeter}) is not one anything is
     * registered under here, so a bean of the application that happens to carry it is no conflict. What counts is
     * whether this extension point was registered before: by an earlier scan of a package that overlaps, or by another
     * {@code @ExtensionScan} of the application.
     * </p>
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        String extensionPointClassName = beanDefinition.getBeanClassName();
        return !getRegistry().containsBeanDefinition(ExtensionPointBeanNameGenerator.genExtensionClassHolderBeanName(extensionPointClassName));
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
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        Class<?> beanClass = loadExtensionPointClass(extensionPointClassName);
        if (beanClass != null) {
            // what a lookup by type that must not create factory beans (@ConditionalOnBean, getBeanNamesForType(type, true, false))
            // goes by
            beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, beanClass);
        }
        // Mark as primary so Spring's standard constructor injection resolves this bean by type
        beanDefinition.setPrimary(true);

        String beanName = ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(extensionPointClassName);
        Objects.requireNonNull(registry).registerBeanDefinition(beanName, beanDefinition);
    }

    private void registerAllMatchedExtensionBeanFactoryDefinition(String extensionPointClassName, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AllMatchedExtensionFactoryBean.class);
        builder.addConstructorArgValue(extensionPointClassName);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, List.class);
        String beanName = ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(extensionPointClassName);
        Objects.requireNonNull(registry).registerBeanDefinition(beanName, beanDefinition);
    }

    private void registerExtensionPointHolderBeanDefinition(String extensionPointClassName, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionPointHolder.class);
        builder.addConstructorArgValue(extensionPointClassName);
        String beanName = ExtensionPointBeanNameGenerator.genExtensionClassHolderBeanName(extensionPointClassName);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    /**
     * Loads the extension point with the class loader of the application, not the one of this library: where an
     * application is started by a class loader of its own (Spring Boot devtools restart, other launchers) the two differ,
     * and the library's cannot see the application's classes.
     */
    private Class<?> loadExtensionPointClass(String extensionPointClassName) {
        ResourceLoader resourceLoader = getResourceLoader();
        ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : null;
        try {
            return ClassUtils.forName(extensionPointClassName, classLoader);
        } catch (ClassNotFoundException | LinkageError e) {
            // the type of the factory bean is then found out by creating it, as for any other factory bean
            logger.debug("Extension point [" + extensionPointClassName + "] cannot be loaded while scanning: " + e);
            return null;
        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
