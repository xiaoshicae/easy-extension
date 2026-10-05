package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

/**
 * Adds the auto-configuration packages (the package of the {@code @SpringBootApplication} class) to the scan, so a
 * Spring Boot application needs no extra scan configuration.
 */
public class ExtensionAutoScanRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (beanFactory != null && AutoConfigurationPackages.has(beanFactory)) {
            List<String> packages = AutoConfigurationPackages.get(beanFactory);
            ExtensionScannerConfigurer.registerScanPackages(registry, packages);
        }
    }
}
