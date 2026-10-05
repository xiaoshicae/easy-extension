package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;

/**
 * Installs the {@code @ExtensionInject} autowire candidate resolver on the bean factory, replacing Spring's
 * standard resolver. A resolver that is not Spring's standard one (set by some other framework) is left alone.
 * <p>Internal infrastructure registered by the auto-configuration; not intended for direct use.</p>
 */
public class ExtensionInjectAutowireCandidateResolverInstaller implements BeanFactoryPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionInjectAutowireCandidateResolverInstaller.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof DefaultListableBeanFactory listableBeanFactory)) {
            return;
        }
        AutowireCandidateResolver current = listableBeanFactory.getAutowireCandidateResolver();
        if (current instanceof ExtensionInjectAutowireCandidateResolver) {
            return;
        }
        if (current.getClass() != ContextAnnotationAutowireCandidateResolver.class) {
            logger.warn("AutowireCandidateResolver {} is not replaced: @ExtensionInject on constructor/method parameters " +
                    "will not resolve to the session-aware extension proxies (fields are not affected)", current.getClass().getName());
            return;
        }
        listableBeanFactory.setAutowireCandidateResolver(new ExtensionInjectAutowireCandidateResolver());
    }
}
