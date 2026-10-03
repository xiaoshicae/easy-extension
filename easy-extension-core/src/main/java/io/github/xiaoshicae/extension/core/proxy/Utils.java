package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.exception.ProxyParamException;

import java.lang.reflect.Modifier;
import java.util.List;

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
