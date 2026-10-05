package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Shared behavior of the extension point facade proxies
 * ({@link FirstMatchedExtPointProxyFactory}, {@link AllMatchedExtPointProxyFactory}).
 */
final class ProxyInvocationSupport {

    private ProxyInvocationSupport() {
    }

    /**
     * {@code toString}, {@code hashCode} and {@code equals} of a facade never depend on the current session.
     */
    static boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    static Object invokeObjectMethod(Object proxy, Method method, Object[] args, String description) {
        return switch (method.getName()) {
            case "toString" -> description;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.toString());
        };
    }

    /**
     * Resolution failed (e.g. session not initialized): surface an unchecked {@link InvokeException}
     * instead of letting the JDK wrap the checked {@link QueryException} into an {@code UndeclaredThrowableException}.
     */
    static InvokeException resolveFailed(Class<?> extensionPointClass, Method method, QueryException cause) {
        return new InvokeException(String.format("call %s.%s failed, %s",
                extensionPointClass.getSimpleName(), method.getName(), cause.getMessage()), cause);
    }

    /**
     * Invoke the resolved target and rethrow whatever the implementation threw, not the reflection wrapper.
     */
    static Object invokeTarget(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
