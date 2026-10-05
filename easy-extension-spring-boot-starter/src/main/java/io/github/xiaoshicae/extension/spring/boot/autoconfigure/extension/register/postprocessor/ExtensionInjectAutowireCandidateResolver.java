package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;

/**
 * Makes {@code @ExtensionInject} work on every injection point Spring autowires (constructor and method
 * parameters, or fields that are also {@code @Autowired}), not only on the fields handled by
 * {@link ExtensionInjectAnnotationBeanPostProcessor}.
 * <p>
 * Without this, a {@code List<X>} parameter would be autowired by Spring as <em>every</em> bean implementing
 * {@code X}, instead of the session-aware all-matched proxy. Only {@code X} and {@code List<X>} of a registered
 * extension point are answered here, with the same proxy beans the field injection uses; any other injection
 * point (a {@code Set<X>}, an unregistered type, ...) is left to Spring's regular autowiring.
 * </p>
 */
class ExtensionInjectAutowireCandidateResolver extends ContextAnnotationAutowireCandidateResolver {

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
        if (descriptor.getAnnotation(ExtensionInject.class) != null) {
            String injectBeanName = ExtensionPointBeanNameGenerator.genInjectBeanName(descriptor.getResolvableType());
            if (injectBeanName != null && getBeanFactory().containsBean(injectBeanName)) {
                return getBeanFactory().getBean(injectBeanName);
            }
        }
        return super.getLazyResolutionProxyIfNecessary(descriptor, beanName);
    }
}
