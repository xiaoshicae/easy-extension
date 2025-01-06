package io.github.xiaoshicae.extension.core.common;

@FunctionalInterface
public interface Matcher<T> {

    /**
     * The Matcher interface is used to determine
     * whether the instance of an extension point implementation takes effect
     * in a single request.
     *
     * @param param for match predict
     * @return if match
     */
    Boolean match(T param);
}
