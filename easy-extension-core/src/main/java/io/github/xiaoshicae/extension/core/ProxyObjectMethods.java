package io.github.xiaoshicae.extension.core;

import java.lang.reflect.Method;

/**
 * {@code toString()}, {@code hashCode()} and {@code equals(Object)} of the proxies the framework hands out.
 * <p>
 * A proxy that stands for "whoever answers this extension point for the current request" (what
 * {@code @ExtensionInject} injects) is a handle, not the implementation: resolving something to answer these three
 * would fail outside a session (a logger, a debugger, a Lombok-generated {@code toString()} or a {@code HashSet} must
 * be able to hold such a proxy), could answer differently from one call to the next, and recurses when the answering
 * implementation prints a field that holds the proxy of its own extension point. So they are answered by identity.
 * </p>
 */
final class ProxyObjectMethods {

    private ProxyObjectMethods() {
    }

    /**
     * Whether {@code method} is one of the methods of {@link Object} a proxy is asked for.
     */
    static boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    /**
     * Answer {@code toString()}, {@code hashCode()} or {@code equals(Object)} of {@code proxy} by identity.
     *
     * @param method      one of the methods of {@link Object} that a proxy is asked for
     * @param description what the proxy stands for, e.g. {@code Extension<PriceExtension>}
     */
    static Object answer(Object proxy, Method method, Object[] args, String description) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> description + "@" + Integer.toHexString(System.identityHashCode(proxy));
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }

    /**
     * Whether {@code method} is {@code proxy.equals(proxy)}. A proxy that forwards {@code equals} to the object it wraps
     * must still be equal to itself, though the wrapped object has never heard of the proxy.
     */
    static boolean isEqualsToItself(Object proxy, Method method, Object[] args) {
        return isObjectMethod(method) && "equals".equals(method.getName()) && args != null && args[0] == proxy;
    }
}
