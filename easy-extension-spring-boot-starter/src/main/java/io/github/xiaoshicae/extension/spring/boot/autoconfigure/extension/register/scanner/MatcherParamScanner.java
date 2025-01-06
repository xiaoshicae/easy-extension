package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.Objects;
import java.util.Set;

public class MatcherParamScanner extends ClassPathBeanDefinitionScanner {
    public MatcherParamScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(MatcherParam.class));
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.size() > 1) {
            throw new IllegalArgumentException(String.format("@MatcherParam more than one scanned in [%s]", String.join(", ", basePackages)));
        }

        return beanDefinitions;
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        String beanClassName = Objects.requireNonNull(holder.getBeanDefinition().getBeanClassName());

        Class<?> beanClass;
        try {
            beanClass = ClassUtils.forName(beanClassName, ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Scan @MatcherParam class not found: " + beanClassName, e);
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MatcherParamHolder.class);
        builder.addConstructorArgValue(beanClass);
        String beanName = ExtensionPointBeanNameGenerator.genMatcherParamClassBeanName(beanClassName);
        if (registry.containsBeanDefinition(beanName)) {
            throw new IllegalArgumentException("Duplicate @MatcherParam instance found ");
        }
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }
}
