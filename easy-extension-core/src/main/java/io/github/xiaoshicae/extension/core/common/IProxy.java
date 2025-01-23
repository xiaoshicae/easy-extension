package io.github.xiaoshicae.extension.core.common;

public interface IProxy<T> {
    /**
     * Get proxy real instance
     */
    T getInstance();
}
