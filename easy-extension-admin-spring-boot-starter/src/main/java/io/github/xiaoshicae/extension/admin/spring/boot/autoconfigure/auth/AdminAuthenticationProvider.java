package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI (Service Provider Interface) for custom authentication in the admin module.
 * <p>
 * Implement this interface to provide custom authentication logic for the admin API.
 * Register your implementation as a Spring bean and it will be automatically picked up.
 * </p>
 *
 * <p>Example: Basic Auth Authentication</p>
 * <pre>{@code
 * @Bean
 * public AdminAuthenticationProvider basicAuthProvider() {
 *     return new AdminAuthenticationProvider() {
 *         @Override
 *         public boolean authenticate(HttpServletRequest request) {
 *             String authHeader = request.getHeader("Authorization");
 *             if (authHeader == null || !authHeader.startsWith("Basic ")) {
 *                 return false;
 *             }
 *             String credentials = new String(Base64.getDecoder()
 *                 .decode(authHeader.substring(6)));
 *             String[] parts = credentials.split(":", 2);
 *             return parts.length == 2
 *                 && "admin".equals(parts[0])
 *                 && "secret".equals(parts[1]);
 *         }
 *     };
 * }
 * }</pre>
 */
public interface AdminAuthenticationProvider {

    /**
     * Check if the incoming request is authenticated.
     *
     * @param request the current HTTP request
     * @return true if authenticated, false otherwise (will result in 401 response)
     */
    boolean authenticate(HttpServletRequest request);

    /**
     * Return the WWW-Authenticate challenge header value.
     * <p>
     * This is sent back to the client when authentication fails.
     * Common values: "Basic realm=\"Easy Extension Admin\"" for Basic Auth,
     * "Bearer realm=\"Easy Extension Admin\"" for Bearer tokens.
     * Returns null if no challenge header is needed.
     * </p>
     *
     * @return the challenge header value, or null
     */
    default String getChallenge() {
        return null;
    }
}
