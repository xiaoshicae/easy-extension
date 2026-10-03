package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.proxy.InvocationExceptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class AllMatchedExtPointProxyFactory<T> {
    private final Class<T> extensionPointClass;
    private final Class<?> extensionPointListClass = ArrayList.class;
    private final IExtensionFactory extensionFactory;

    public AllMatchedExtPointProxyFactory(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.extensionFactory = extensionFactory;
    }

    @SuppressWarnings("unchecked")
    public List<T> getProxy() {
        return (List<T>) Proxy.newProxyInstance(
                extensionPointListClass.getClassLoader(),
                extensionPointListClass.getInterfaces(),
                new AllMatchedExtensionInvocationHandler<>(extensionPointClass, extensionFactory)
        );
    }

    public static class AllMatchedExtensionInvocationHandler<T> implements InvocationHandler {
        private final Class<T> extensionPointClass;
        private final IExtensionFactory extensionFactory;
        private final boolean legacyWrapping = InvocationExceptions.legacyWrapping();

        public AllMatchedExtensionInvocationHandler(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
            this.extensionPointClass = extensionPointClass;
            this.extensionFactory = extensionFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            List<T> allMatchedExtension;
            try {
                allMatchedExtension = extensionFactory.getAllMatchedExtension(extensionPointClass);
            } catch (QueryException e) {
                throw InvocationExceptions.queryFailure(method, "List<" + extensionPointClass.getSimpleName() + ">", e, legacyWrapping);
            }
            return InvocationExceptions.invoke(method, allMatchedExtension, args, legacyWrapping);
        }
    }
}
