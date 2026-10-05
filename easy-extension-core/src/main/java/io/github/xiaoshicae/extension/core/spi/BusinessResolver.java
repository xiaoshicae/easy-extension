package io.github.xiaoshicae.extension.core.spi;

import java.util.Optional;

/**
 * Maps the request parameter straight to a business code (typically {@code param.getBizCode()}), which is a
 * hash lookup instead of asking every business to {@code match}. When a context has a resolver, businesses
 * need not implement {@code Matcher}, and their {@code Matcher} is ignored. An empty result means "no
 * business", it does not fall back to matching.
 *
 * @param <T> matcher param type
 */
@FunctionalInterface
public interface BusinessResolver<T> {

    /**
     * @return the code of the business serving this request, or empty if none
     */
    Optional<String> resolve(T param);
}
