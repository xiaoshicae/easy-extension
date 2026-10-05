package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class InstanceScanner extends ClassPathBeanDefinitionScanner {
    public InstanceScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(Ability.class));
        addIncludeFilter(new AnnotationTypeFilter(Business.class));
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPointDefaultImplementation.class));
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        super.registerBeanDefinition(holder, registry);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(InstanceHolder.class);
        // reference the bean registered above; passing the BeanDefinition itself would make Spring
        // create a second, separate instance as an inner bean of the holder
        builder.addConstructorArgReference(holder.getBeanName());
        registry.registerBeanDefinition(holder.getBeanName()+"#InstanceHolder", builder.getBeanDefinition());
    }
}
