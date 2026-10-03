package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

import org.springframework.aop.framework.AopProxyUtils;

public class InstanceHolder {
    private final Object instance;

    public InstanceHolder(Object instance) {
        this.instance = instance;
    }

    /**
     * The bean to register. When Spring wraps it in an AOP proxy (for example because of {@code @Transactional},
     * {@code @Cacheable} or {@code @Async}) this is the proxy, so that the advice keeps applying to calls made
     * by the framework.
     */
    public Object getInstance() {
        return instance;
    }

    /**
     * The class behind {@link #getInstance()}. The annotations and implemented extension points are read from it:
     * a JDK proxy class carries none of them and {@code @Business} / {@code @Ability} are not {@code @Inherited},
     * so a CGLIB subclass does not either.
     *
     * @since 4.0
     */
    public Class<?> getTargetClass() {
        return AopProxyUtils.ultimateTargetClass(instance);
    }
}
