package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.proxy.InvocationExceptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class FirstMatchedExtPointProxyFactory<T> {
    private final Class<T> extensionPointClass;
    private final IExtensionFactory extensionFactory;

    public FirstMatchedExtPointProxyFactory(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.extensionFactory = extensionFactory;
    }

    @SuppressWarnings("unchecked")
    public T getProxy() {
        return (T) Proxy.newProxyInstance(
                extensionPointClass.getClassLoader(),
                new Class[]{extensionPointClass},
                new FirstMatchedExtPointInvocationHandler<>(extensionPointClass, extensionFactory)
        );
    }

    public static class FirstMatchedExtPointInvocationHandler<T> implements InvocationHandler {
        private final Class<T> extensionPointClass;
        private final IExtensionFactory extensionFactory;
        private final boolean legacyWrapping = InvocationExceptions.legacyWrapping();

        public FirstMatchedExtPointInvocationHandler(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
            this.extensionPointClass = extensionPointClass;
            this.extensionFactory = extensionFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ProxyObjectMethods.isObjectMethod(method)) {
                return ProxyObjectMethods.answer(proxy, method, args, "Extension<" + extensionPointClass.getSimpleName() + ">");
            }
            T firstMatchedExtension;
            try {
                firstMatchedExtension = extensionFactory.getFirstMatchedExtension(extensionPointClass);
            } catch (QueryException e) {
                throw InvocationExceptions.queryFailure(method, extensionPointClass.getSimpleName(), e, legacyWrapping);
            }
            return InvocationExceptions.invoke(method, firstMatchedExtension, args, legacyWrapping);
        }
    }
}
