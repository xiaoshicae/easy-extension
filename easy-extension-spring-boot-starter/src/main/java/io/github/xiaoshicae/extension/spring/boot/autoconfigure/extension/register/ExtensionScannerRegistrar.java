package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor.ExtensionInjectAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtensionScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes mapperScanAttrs = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ExtensionScan.class.getName()));
        if (mapperScanAttrs != null) {
            registerBeanDefinitions(importingClassMetadata, mapperScanAttrs, registry, generateBaseBeanName(importingClassMetadata, 0));
        }
        registerExtensionInjectBeanDefinitions(registry);
    }

    void registerBeanDefinitions(AnnotationMetadata annoMeta, AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry, String beanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionScannerConfigurer.class);
        addPropertyPackages(annoMeta, annoAttrs, builder, "scanPackages");
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    void addPropertyPackages(AnnotationMetadata annoMeta, AnnotationAttributes annoAttrs, BeanDefinitionBuilder builder, String packageName) {
        List<String> basePackages = new ArrayList<>(Arrays.stream(annoAttrs.getStringArray(packageName)).filter(StringUtils::hasText).toList());
        if (basePackages.isEmpty()) {
            basePackages.add(getDefaultBasePackage(annoMeta));
        }
        builder.addPropertyValue(packageName, StringUtils.collectionToCommaDelimitedString(basePackages));
    }

    void registerExtensionInjectBeanDefinitions(BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionInjectAnnotationBeanPostProcessor.class);
        registry.registerBeanDefinition(ExtensionInjectAnnotationBeanPostProcessor.class.getName(), builder.getBeanDefinition());
    }

    private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        return importingClassMetadata.getClassName() + "#" + ExtensionScannerRegistrar.class.getSimpleName() + "#" + index;
    }

    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }
}
