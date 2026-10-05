package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles {@link ExtensionScan}: its packages, and the package of the annotated class, join the scan.
 */
public class ExtensionScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ExtensionScan.class.getName()));
        List<String> packages = new ArrayList<>();
        if (attributes != null) {
            packages.addAll(Arrays.asList(attributes.getStringArray("basePackages")));
        }
        packages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        ExtensionScannerConfigurer.registerScanPackages(registry, packages);
    }
}
