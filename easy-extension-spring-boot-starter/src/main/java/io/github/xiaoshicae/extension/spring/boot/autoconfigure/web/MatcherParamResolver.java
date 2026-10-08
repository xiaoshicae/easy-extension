package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Derives the request parameter that businesses and abilities match on from the HTTP request. Declare one as a bean
 * and every MVC request is resolved and bound to its thread automatically, and unbound when it completes.
 * <p>
 * It returns the type {@code T} that the matchers accept. When a business is identified by its code alone, {@code T}
 * can be the code itself: a {@code MatcherParamResolver<String>} reading a header, together with a
 * {@link io.github.xiaoshicae.extension.core.spi.BusinessResolver} that is {@code Optional::ofNullable}; the businesses
 * then need no {@code Matcher}. The result is handed to the context as it is, {@code null} included: a
 * {@code BusinessResolver} can read it as "no business", while a {@code Matcher} would receive the {@code null}.
 * </p>
 * <p>
 * The one bean serves every endpoint. Endpoints that do not use extension points are kept out of it with
 * {@code easy-extension.session-include-path-patterns} / {@code easy-extension.session-exclude-path-patterns}.
 * </p>
 *
 * @param <T> matcher param type
 */
@FunctionalInterface
public interface MatcherParamResolver<T> {

    /**
     * The request parameter for this HTTP request, typically built from headers, the path or the authenticated user.
     */
    T resolve(HttpServletRequest request);
}
