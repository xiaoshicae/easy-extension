package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.EnumSet;

/**
 * Servlet support: a {@link MatcherParamResolver} bean turns on automatic binding per request, and a safety-net filter
 * drops any binding left on the request thread.
 */
@AutoConfiguration(after = EasyExtensionAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(ExtensionContext.class)
public class EasyExtensionWebAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(EasyExtensionWebAutoConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "easy-extension.enable-session-auto-cleanup", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SessionCleanupFilter> sessionCleanupFilterRegistration(ExtensionContext<?> extensionContext) {
        FilterRegistrationBean<SessionCleanupFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionCleanupFilter(extensionContext));
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setName("easyExtensionSessionCleanupFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnBean(MatcherParamResolver.class)
    static class SessionInterceptorConfiguration {

        @Bean
        @SuppressWarnings({"rawtypes", "unchecked"})
        WebMvcConfigurer extensionSessionInterceptorConfigurer(ExtensionContext context, MatcherParamResolver resolver,
                                                              EasyExtensionConfigurationProperties properties) {
            ExtensionSessionInterceptor interceptor = new ExtensionSessionInterceptor(context, resolver);
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    InterceptorRegistration registration = registry.addInterceptor(interceptor).order(Ordered.HIGHEST_PRECEDENCE)
                            .excludePathPatterns(properties.getSessionExcludePathPatterns());
                    if (!properties.getSessionIncludePathPatterns().isEmpty()) {
                        registration.addPathPatterns(properties.getSessionIncludePathPatterns());
                    }
                    logger.info("[Easy Extension] HTTP binding is on: for Spring MVC requests matching {} (excluding {}), the "
                                    + "MatcherParamResolver bean derives the param, the request thread is bound to its business "
                                    + "from preHandle until the request completes. Servlet filters and other threads are not bound",
                            properties.getSessionIncludePathPatterns().isEmpty() ? "[/**]" : properties.getSessionIncludePathPatterns(),
                            properties.getSessionExcludePathPatterns());
                }
            };
        }
    }

    /**
     * Without a {@link MatcherParamResolver} bean the starter binds no HTTP request: say so once, because it is the first
     * thing to check when an extension point fails with {@code NO_BINDING}. An {@link ExtensionSessionInterceptor}
     * registered by hand still binds its paths, which the message points out.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnMissingBean(MatcherParamResolver.class)
    static class NoHttpBindingHint {

        @Bean
        SmartInitializingSingleton extensionHttpBindingHint() {
            return () -> logger.info("[Easy Extension] HTTP requests are not bound to a business by the starter: there is no "
                    + "MatcherParamResolver bean. Declare one to bind every Spring MVC request automatically, or bind yourself "
                    + "where a call starts, e.g. context.runWith(param, () -> ...); paths covered by an ExtensionSessionInterceptor "
                    + "you registered yourself are bound by it");
        }
    }
}
