package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * Safety net for pooled request threads: whatever happens, no binding outlives the request on the thread that served it.
 * (Bindings are normally closed by whoever opened them; this catches the ones that were not.)
 */
public class SessionCleanupFilter implements Filter {

    private final ExtensionContext<?> extensionContext;

    public SessionCleanupFilter(ExtensionContext<?> extensionContext) {
        this.extensionContext = extensionContext;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            extensionContext.clear();
        }
    }
}
