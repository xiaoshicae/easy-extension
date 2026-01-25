package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for Easy Extension web support.
 *
 * <p>This configuration is only activated when:
 * <ul>
 *   <li>The application is a servlet-based web application</li>
 *   <li>The property {@code easy-extension.enable-session-auto-cleanup} is not set to {@code false}</li>
 * </ul>
 *
 * <p>It registers a {@link SessionCleanupFilter} that automatically cleans up
 * the extension session after each HTTP request to prevent ThreadLocal memory leaks.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "easy-extension.enable-session-auto-cleanup", havingValue = "true", matchIfMissing = true)
public class EasyExtensionWebAutoConfiguration {

    /**
     * Registers the session cleanup filter with the highest precedence.
     *
     * @param extensionContext the extension context to clean up
     * @param properties the configuration properties
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<SessionCleanupFilter> sessionCleanupFilterRegistration(
            IExtensionContext<?> extensionContext,
            EasyExtensionConfigurationProperties properties) {

        FilterRegistrationBean<SessionCleanupFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionCleanupFilter(extensionContext));
        registration.addUrlPatterns("/*");
        registration.setName("easyExtensionSessionCleanupFilter");
        // Set to lowest precedence so it wraps around all other filters
        // This ensures cleanup happens after all request processing is complete
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
