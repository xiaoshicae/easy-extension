package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInvocation;
import io.github.xiaoshicae.extension.core.proxy.InvocationExceptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Wraps an extension implementation so that every call to it passes through the registered
 * {@link ExtensionInterceptor}s.
 */
final class InterceptingProxy {

    private static final Object[] NO_ARGUMENTS = new Object[0];

    private InterceptingProxy() {
    }

    @SuppressWarnings("unchecked")
    static <E> E create(Class<E> extensionPoint, String code, E implementation, List<ExtensionInterceptor> interceptors) {
        Handler handler = new Handler(extensionPoint, code, implementation, interceptors.toArray(new ExtensionInterceptor[0]));
        return (E) Proxy.newProxyInstance(extensionPoint.getClassLoader(), new Class<?>[]{extensionPoint}, handler);
    }

    private static final class Handler implements InvocationHandler {
        private final Class<?> extensionPoint;
        private final String code;
        private final Object implementation;
        private final ExtensionInterceptor[] interceptors;
        private final boolean legacyWrapping = InvocationExceptions.legacyWrapping();

        private Handler(Class<?> extensionPoint, String code, Object implementation, ExtensionInterceptor[] interceptors) {
            this.extensionPoint = extensionPoint;
            this.code = code;
            this.implementation = implementation;
            this.interceptors = interceptors;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object[] arguments = args == null ? NO_ARGUMENTS : args;
            if (ProxyObjectMethods.isObjectMethod(method)) {
                // toString / hashCode / equals are not extension calls. The implementation has never heard of the
                // proxy, so it would say that the proxy is not equal to itself.
                if (ProxyObjectMethods.isEqualsToItself(proxy, method, args)) {
                    return true;
                }
                return InvocationExceptions.invoke(method, implementation, arguments, legacyWrapping);
            }
            return new Invocation(this, method, arguments, 0).proceed();
        }
    }

    private static final class Invocation implements ExtensionInvocation {
        private final Handler handler;
        private final Method method;
        private final Object[] arguments;
        private final int index;

        private Invocation(Handler handler, Method method, Object[] arguments, int index) {
            this.handler = handler;
            this.method = method;
            this.arguments = arguments;
            this.index = index;
        }

        @Override
        public Class<?> extensionPoint() {
            return handler.extensionPoint;
        }

        @Override
        public String implementationCode() {
            return handler.code;
        }

        @Override
        public Object implementation() {
            return handler.implementation;
        }

        @Override
        public Method method() {
            return method;
        }

        @Override
        public Object[] arguments() {
            return arguments;
        }

        @Override
        public Object proceed() throws Throwable {
            if (index == handler.interceptors.length) {
                return InvocationExceptions.invoke(method, handler.implementation, arguments, handler.legacyWrapping);
            }
            return handler.interceptors[index].intercept(new Invocation(handler, method, arguments, index + 1));
        }
    }
}
