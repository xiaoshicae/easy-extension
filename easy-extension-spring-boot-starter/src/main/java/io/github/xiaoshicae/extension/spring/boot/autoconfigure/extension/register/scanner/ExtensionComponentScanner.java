package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Scans the packages once for everything the framework registers: extension point interfaces,
 * the matcher param class, abilities, businesses and the extension point default implementation.
 * <p>
 * Each candidate is handed to the registration logic of the specialised scanner
 * ({@link ExtensionPointScanner}, {@link ClassScanner}, {@link InstanceScanner}) that owns its kind,
 * so the classpath is walked once instead of once per kind.
 * </p>
 */
public class ExtensionComponentScanner extends ClassPathBeanDefinitionScanner {
    private final ExtensionPointScanner extensionPointScanner;
    private final ClassScanner classScanner;
    private final InstanceScanner instanceScanner;

    public ExtensionComponentScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPoint.class));
        addIncludeFilter(new AnnotationTypeFilter(MatcherParam.class));
        addIncludeFilter(new AnnotationTypeFilter(Ability.class));
        addIncludeFilter(new AnnotationTypeFilter(Business.class));
        addIncludeFilter(new AnnotationTypeFilter(ExtensionPointDefaultImplementation.class));
        this.extensionPointScanner = new ExtensionPointScanner(registry);
        this.classScanner = new ClassScanner(registry);
        this.instanceScanner = new InstanceScanner(registry);
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        AnnotationMetadata metadata = beanDefinition.getMetadata();
        if (metadata.isInterface()) {
            // extension points are interfaces, which the default check rejects
            return metadata.isIndependent() && metadata.isAnnotated(ExtensionPoint.class.getName());
        }
        // everything else must be a concrete class carrying one of the other annotations
        // (an @ExtensionPoint on a class is not an extension point)
        return super.isCandidateComponent(beanDefinition) && (isMatcherParam(metadata) || isInstance(metadata));
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder holder, BeanDefinitionRegistry registry) {
        AnnotationMetadata metadata = ((AnnotatedBeanDefinition) holder.getBeanDefinition()).getMetadata();
        if (metadata.isInterface()) {
            extensionPointScanner.registerBeanDefinition(holder, registry);
            return;
        }
        if (isMatcherParam(metadata)) {
            classScanner.registerBeanDefinition(holder, registry);
        }
        if (isInstance(metadata)) {
            instanceScanner.registerBeanDefinition(holder, registry);
        }
    }

    private static boolean isMatcherParam(AnnotationMetadata metadata) {
        return metadata.isAnnotated(MatcherParam.class.getName());
    }

    /** An ability, a business or the extension point default implementation. */
    private static boolean isInstance(AnnotationMetadata metadata) {
        return metadata.isAnnotated(Ability.class.getName())
                || metadata.isAnnotated(Business.class.getName())
                || metadata.isAnnotated(ExtensionPointDefaultImplementation.class.getName());
    }
}
