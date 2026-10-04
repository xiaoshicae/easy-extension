package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class ClassScanner extends ClassPathBeanDefinitionScanner {
    public ClassScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(MatcherParam.class));
    }

    /**
     * Whether the class is yet to be registered: not by an earlier scan of a package that overlaps, nor by another
     * {@code @ExtensionScan} of the application. A bean of the application that carries the name the scanner gives the
     * class is no conflict, nothing is registered under that name.
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        return !getRegistry().containsBeanDefinition(holderBeanName(beanDefinition));
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ClassHolder.class);
        builder.addConstructorArgValue(holder.getBeanDefinition().getBeanClassName());
        registry.registerBeanDefinition(holderBeanName(holder.getBeanDefinition()), builder.getBeanDefinition());
    }

    /**
     * By the full class name: two classes of the same simple name in different packages are two classes (and something
     * the application is told about), not one name registered twice.
     */
    private static String holderBeanName(BeanDefinition candidate) {
        return candidate.getBeanClassName() + "#ClassHolder";
    }
}
