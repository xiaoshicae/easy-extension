package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.proxy.AllMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.FirstMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ExtensionPointScanner extends ClassPathBeanDefinitionScanner {
    private static final Logger LOGGER = Logger.getLogger(ExtensionPointScanner.class.getName());

    public ExtensionPointScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            LOGGER.warning(() -> "No extension point was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
        }
        return beanDefinitions;
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        String extensionPointClassName = holder.getBeanDefinition().getBeanClassName();

        // register first matched extension factory bean
        registerFirstMatchedExtensionFactoryBeanDefinition(extensionPointClassName, registry);

        // register all matched extension factory bean
        registerAllMatchedExtensionBeanFactoryDefinition(extensionPointClassName, registry);

        // register extension class holder
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
        registry.registerBeanDefinition(extensionPointClassName + "#ExtensionPointClassHolder", builder.getBeanDefinition());
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        if (beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent()) {
            return true;
        }

        LOGGER.config(() -> String.format("Skipping register extension point proxy: [%s]. Bean is not an interface or nested class.", beanDefinition.getBeanClassName()));
        return false;
    }

    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        }

        LOGGER.config(() -> String.format("Skipping register extension point proxy: [%s] with bean name: [%s]" + ". Bean already defined with the same name!", beanName, beanDefinition.getBeanClassName()));
        return false;
    }
}
