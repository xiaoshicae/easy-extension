package io.github.xiaoshicae.extension.core.proxy;

/**
 * Implemented by the dynamic proxies the framework creates for businesses, abilities and default implementations.
 * <p>
 * The methods of this interface are answered by the proxy itself, so an extension point must not declare a method
 * with the same name and parameters ({@code getInstance()}, {@code getTargetClass()}): with another return type the
 * proxy cannot be created, with the same return type the call never reaches the implementation.
 * </p>
 *
 * @param <T> type of the proxied instance
 */
public interface IProxy<T> {
    /**
     * Get proxy real instance
     */
    T getInstance();

    /**
     * Get the class that carries the registration metadata of the proxied implementation: its annotations
     * and the extension points it implements.
     * <p>
     * This is not necessarily {@code getInstance().getClass()}: when the instance is itself a proxy created by
     * a container (for example a Spring AOP proxy), the target class is the user class behind it.
     * </p>
     *
     * @return the class behind the instance
     * @since 4.0
     */
    default Class<?> getTargetClass() {
        return getInstance().getClass();
    }
}
