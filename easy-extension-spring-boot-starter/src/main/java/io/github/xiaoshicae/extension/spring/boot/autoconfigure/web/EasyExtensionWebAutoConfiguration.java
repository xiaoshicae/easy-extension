package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
                    registry.addInterceptor(interceptor).order(Ordered.HIGHEST_PRECEDENCE)
                            .excludePathPatterns(properties.getSessionExcludePathPatterns());
                }
            };
        }
    }
}
