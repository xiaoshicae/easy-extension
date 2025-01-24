package io.github.xiaoshicae.extension.core;

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

        public AllMatchedExtensionInvocationHandler(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
            this.extensionPointClass = extensionPointClass;
            this.extensionFactory = extensionFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            List<T> allMatchedExtension = extensionFactory.getAllMatchedExtension(extensionPointClass);
            return method.invoke(allMatchedExtension, args);
        }
    }
}
