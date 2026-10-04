package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

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
    private final PathPattern adminApiPattern;

    public AdminAuthenticationFilter(List<AdminAuthenticationProvider> providers, String adminPath) {
        this.providers = providers;
        this.adminApiPathPrefix = adminPath != null ? adminPath + "/easy-extension-api" : "/easy-extension-admin/easy-extension-api";
        // Spring MVC matches a mapping against the path within the application, the way a PathPattern does: context
        // path removed, ";params" dropped, percent-encoding decoded, a missing leading slash added to the mapping.
        // The filter has to agree with it on what the admin API is, or the API can be reached without passing the filter.
        String prefix = adminApiPathPrefix.startsWith("/") ? adminApiPathPrefix : "/" + adminApiPathPrefix;
        this.adminApiPattern = PathPatternParser.defaultInstance.parse(prefix + "/**");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (providers == null || providers.isEmpty()) {
            // No authentication configured, pass through (backward compatible)
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate requests to the admin API path
        if (!isAdminApi(request)) {
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

    /**
     * Whether the request is for the admin API. A request counts as one if any of the ways of reading its path says
     * so (the raw request URI, the path as Spring MVC reads it, the path as the servlet container normalized it), so
     * that the set of protected requests can only grow with every additional reading.
     */
    private boolean isAdminApi(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(adminApiPathPrefix)) {
            return true;
        }
        boolean alreadyParsed = ServletRequestPathUtils.hasParsedRequestPath(request);
        try {
            RequestPath path = alreadyParsed ? ServletRequestPathUtils.getParsedRequestPath(request) : ServletRequestPathUtils.parseAndCache(request);
            if (adminApiPattern.matches(path.pathWithinApplication())) {
                return true;
            }
            String servletPath = request.getServletPath();
            String pathInfo = request.getPathInfo();
            return adminApiPattern.matches(PathContainer.parsePath((servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo)));
        } catch (IllegalArgumentException e) {
            // a path that cannot be decoded: do not let it past the filter
            return true;
        } finally {
            if (!alreadyParsed) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            }
        }
    }
}
