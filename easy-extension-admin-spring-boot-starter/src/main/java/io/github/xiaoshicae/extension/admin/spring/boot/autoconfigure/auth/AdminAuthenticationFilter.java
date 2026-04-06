package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Filter that enforces authentication on admin API endpoints using the configured {@link AdminAuthenticationProvider}.
 * <p>
 * If no provider is configured, all requests pass through (backward compatible).
 * If multiple providers are configured, ALL of them must return true for the request to succeed.
 * </p>
 */
public class AdminAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthenticationFilter.class);

    private final List<AdminAuthenticationProvider> providers;
    private final String adminApiPathPrefix;

    public AdminAuthenticationFilter(List<AdminAuthenticationProvider> providers, String adminPath) {
        this.providers = providers;
        this.adminApiPathPrefix = adminPath != null ? adminPath + "/easy-extension-api" : "/easy-extension-admin/easy-extension-api";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only authenticate requests to the admin API path
        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith(adminApiPathPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (providers == null || providers.isEmpty()) {
            // No authentication configured, pass through (backward compatible)
            filterChain.doFilter(request, response);
            return;
        }

        boolean authenticated = providers.stream().allMatch(provider -> provider.authenticate(request));

        if (!authenticated) {
            logger.warn("Authentication failed for request: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            String timestamp = Instant.now().toString();
            response.getWriter().write("{\"code\":\"B00000\",\"msg\":\"Authentication required\",\"data\":null,\"total\":null,\"timestamp\":\"" + timestamp + "\"}");

            // Add challenge headers from all providers
            for (AdminAuthenticationProvider provider : providers) {
                String challenge = provider.getChallenge();
                if (challenge != null && !challenge.isEmpty()) {
                    response.addHeader("WWW-Authenticate", challenge);
                }
            }
            return;
        }

        filterChain.doFilter(request, response);
    }
}
