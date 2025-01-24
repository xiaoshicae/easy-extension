package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.exception.ProxyParamException;

import java.lang.reflect.Modifier;
import java.util.List;

public class Utils {
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
