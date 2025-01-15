package io.github.xiaoshicae.extension.core.proxy;

public interface IProxy<T> {
    /**
     * Get proxy real instance
     */
    T getInstance();
}
