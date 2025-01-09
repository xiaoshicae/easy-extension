package io.github.xiaoshicae.extension.core.common;


/**
 * The design purpose of the Matcher interface is to determine
 * whether an instance is effective during a single request process.
 */
@FunctionalInterface
public interface Matcher<T> {

    /**
     * Match predict.
     *
     * @param param for match predict
     * @return if match
     */
    Boolean match(T param);
}
