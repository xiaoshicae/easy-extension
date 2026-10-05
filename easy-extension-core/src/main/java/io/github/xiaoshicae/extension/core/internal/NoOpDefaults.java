package io.github.xiaoshicae.extension.core.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * No-op default for extension points whose abstract methods all return {@code void}; {@code default} methods run as written.
 */
final class NoOpDefaults {

    private NoOpDefaults() {
    }

    static boolean canBeNoOp(Class<?> point) {
        for (Method method : point.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers()) && method.getReturnType() != void.class) {
                return false;
            }
        }
        return true;
    }

    static Object create(Class<?> point) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> "NoOpDefault<" + point.getName() + ">";
                };
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return null;
        };
        return Proxy.newProxyInstance(point.getClassLoader(), new Class<?>[]{point}, handler);
    }
}
