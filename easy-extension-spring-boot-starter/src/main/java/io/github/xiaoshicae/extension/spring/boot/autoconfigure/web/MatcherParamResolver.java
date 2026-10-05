package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Derives the request parameter that businesses and abilities match on from the HTTP request. Declare one as a bean
 * and every MVC request is resolved and bound to its thread automatically, and unbound when it completes.
 *
 * @param <T> matcher param type
 */
@FunctionalInterface
public interface MatcherParamResolver<T> {

    T resolve(HttpServletRequest request);
}
