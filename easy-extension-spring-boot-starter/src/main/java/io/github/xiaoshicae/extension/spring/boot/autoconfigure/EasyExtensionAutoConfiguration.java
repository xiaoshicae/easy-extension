package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.spi.BusinessResolver;
import io.github.xiaoshicae.extension.core.spi.BusinessSelector;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.ExtensionAutoScanRegistrar;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor.ExtensionInjectAnnotationBeanPostProcessor;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor.ExtensionInjectAutowireCandidateResolverInstaller;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.task.ExtensionTaskDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;

/**
 * Registers the {@link ExtensionContext}, scans the auto-configuration packages for extension points and their
 * providers, and wires {@code @ExtensionInject}.
 */
@AutoConfiguration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
@Import(ExtensionAutoScanRegistrar.class)
public class EasyExtensionAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EasyExtensionAutoConfiguration.class);

    /**
     * Declared with the raw type on purpose: Spring then matches injection points of any {@code ExtensionContext<X>}.
     * Not lazy, whatever {@code spring.main.lazy-initialization} says: the context is built in a callback that only
     * runs for singletons instantiated at startup.
     */
    @Bean
    @Lazy(false)
    @ConditionalOnMissingBean(ExtensionContext.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public DeferredExtensionContext extensionContext(ListableBeanFactory beanFactory,
                                                     EasyExtensionConfigurationProperties properties,
                                                     ObjectProvider<BusinessResolver> businessResolver,
                                                     ObjectProvider<BusinessSelector> businessSelector) {
        return new DeferredExtensionContext(beanFactory, properties, businessResolver, businessSelector);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static ExtensionInjectAnnotationBeanPostProcessor extensionInjectAnnotationBeanPostProcessor() {
        return new ExtensionInjectAnnotationBeanPostProcessor();
    }

    /**
     * Lets {@code @ExtensionInject} work on constructor and method parameters as well.
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static ExtensionInjectAutowireCandidateResolverInstaller extensionInjectAutowireCandidateResolverInstaller() {
        return new ExtensionInjectAutowireCandidateResolverInstaller();
    }

    /**
     * With {@code easy-extension.async-propagation=true}, tasks submitted to the task executors Spring Boot configures
     * ({@code @Async}, {@code applicationTaskExecutor}) run with the business bound on the submitting thread. Spring Boot
     * combines this with any other {@code TaskDecorator} bean.
     */
    @Bean
    @ConditionalOnProperty(name = "easy-extension.async-propagation", havingValue = "true")
    public ExtensionTaskDecorator extensionTaskDecorator(ExtensionContext<?> context) {
        logger.info("[Easy Extension] async propagation is on: tasks run by Spring task executors use the business "
                + "bound on the thread that submitted them");
        return new ExtensionTaskDecorator(context);
    }
}
