package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Objects;

public class InstanceScanner extends ClassPathBeanDefinitionScanner {
    public InstanceScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(Ability.class));
        addIncludeFilter(new AnnotationTypeFilter(Business.class));
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPointDefaultImplementation.class));
    }

    /**
     * Where the registry has a definition of the class already, the scanner leaves it at that: the class is not
     * registered a second time. That is the case for a business that is also a {@code @Component}, which the
     * application's own {@code @ComponentScan} registered, and for a package that is scanned twice. The extension
     * context still has to be told about the class, so the holder that does so is registered here.
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        }
        // Another class, then: a definition the application made on purpose under that name, which overrides the
        // scanned class like it does for any scanned component. There is nothing to hand over.
        BeanDefinition existing = getRegistry().getBeanDefinition(beanName);
        if (Objects.equals(existing.getBeanClassName(), beanDefinition.getBeanClassName())) {
            registerHolder(beanName, getRegistry());
        }
        return false;
    }

    @Override
    public void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        super.registerBeanDefinition(holder, registry);
        registerHolder(holder.getBeanName(), registry);
    }

    private static void registerHolder(String beanName, BeanDefinitionRegistry registry) {
        String holderName = beanName + "#InstanceHolder";
        if (registry.containsBeanDefinition(holderName)) {
            return;
        }
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(InstanceHolder.class);
        // A reference to the bean registered above. Passing holder.getBeanDefinition() instead would make Spring
        // treat it as an inner bean: a second instance, unrelated to the one the application context manages.
        builder.addConstructorArgReference(beanName);
        registry.registerBeanDefinition(holderName, builder.getBeanDefinition());
    }
}
