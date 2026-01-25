package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * A servlet filter that automatically cleans up the extension session after each request.
 * This prevents ThreadLocal memory leaks in thread pool environments (e.g., web containers).
 *
 * <p>The filter calls {@link IExtensionContext#removeSession()} in a finally block to ensure
 * cleanup happens even if an exception occurs during request processing.</p>
 */
public class SessionCleanupFilter implements Filter {

    private final IExtensionContext<?> extensionContext;

    public SessionCleanupFilter(IExtensionContext<?> extensionContext) {
        this.extensionContext = extensionContext;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            extensionContext.removeSession();
        }
    }
}
