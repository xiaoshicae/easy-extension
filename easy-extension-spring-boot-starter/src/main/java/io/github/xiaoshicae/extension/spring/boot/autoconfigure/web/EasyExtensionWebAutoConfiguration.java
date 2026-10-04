package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.EnumSet;

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
 * the extension session after each HTTP request (its first, async and error dispatches) to prevent ThreadLocal
 * memory leaks.</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "easy-extension.enable-session-auto-cleanup", havingValue = "true", matchIfMissing = true)
public class EasyExtensionWebAutoConfiguration {

    /**
     * Registers the session cleanup filter with the highest precedence.
     *
     * @param extensionContext the extension context to clean up
     * @return the filter registration bean
     */
    @Bean
    public FilterRegistrationBean<SessionCleanupFilter> sessionCleanupFilterRegistration(
            IExtensionContext<?> extensionContext) {

        FilterRegistrationBean<SessionCleanupFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SessionCleanupFilter(extensionContext));
        registration.addUrlPatterns("/*");
        registration.setName("easyExtensionSessionCleanupFilter");
        // Set to highest precedence so it wraps around all other filters
        // This ensures cleanup happens after all request processing is complete (in the finally block)
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        // A plain filter is mapped to REQUEST only. The container serves more than the first dispatch of a request on
        // its worker threads: the async dispatch (Spring MVC runs its interceptors again for it) and the error dispatch.
        // A session started there would stay on the pooled thread and serve the next request that does not start its
        // own. FORWARD and INCLUDE stay out: they run inside a dispatch that is covered, and cleaning up after them
        // would end the session of the code that forwards.
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        return registration;
    }
}
