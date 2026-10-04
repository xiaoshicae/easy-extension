package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.exception.ProxyParamException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
    /**
     * The class of {@code instance}, or {@code null} for a null instance, so that the constructors that take no
     * target class keep reporting a null instance the way they always did (by {@link #validateInstance}).
     */
    static Class<?> classOf(Object instance) {
        return instance == null ? null : instance.getClass();
    }

    static void validateTargetClass(Class<?> targetClass) throws ProxyParamException {
        if (targetClass == null) {
            throw new ProxyParamException("target class should not be null");
        }
    }

    /**
     * The proxy answers the methods of its framework interface ({@code code()}, {@code priority()},
     * {@code getInstance()} ...) itself and forwards everything else to the implementation. An extension point that
     * declares such a method would be answered by the framework and its implementation never asked, so it is refused.
     * {@code match} is exempt: the proxy forwards it to the implementation, which is what the extension point means.
     *
     * @param kind           what the proxy is for, with its article, e.g. {@code "a business"}
     * @param proxyInterface the framework interface of the proxy
     * @param implExtPoints  the extension points the proxy implements
     * @throws ProxyParamException if an extension point declares a method that the proxy interface declares too
     */
    static void validateNoClash(String kind, Class<?> proxyInterface, List<Class<?>> implExtPoints) throws ProxyParamException {
        for (Class<?> extensionPoint : implExtPoints) {
            for (Method method : extensionPoint.getMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || "match".equals(method.getName())) {
                    continue;
                }
                try {
                    proxyInterface.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    continue;
                }
                String signature = method.getName() + Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
                throw new ProxyParamException(String.format(
                        "extension point [%s] declares method %s, which the framework answers itself for %s; rename the method",
                        extensionPoint.getName(), signature, kind));
            }
        }
    }

    public static void validateInstance(Object instance, List<Class<?>> implExtPoints) throws ProxyParamException {
        for (Class<?> implExtPoint : implExtPoints) {
            if (!implExtPoint.isInterface()) {
                throw new ProxyParamException("The extension point should be an interface: " + implExtPoint.getName());
            }
            if (!implExtPoint.isInstance(instance)) {
                throw new ProxyParamException("The instance does not implement the extension point: " + implExtPoint.getName());
            }
            if (!Modifier.isPublic(implExtPoint.getModifiers())) {
                throw new ProxyParamException(String.format("Modifier of extension point [%s] should be public", implExtPoint.getName()));
            }
        }
    }
}
