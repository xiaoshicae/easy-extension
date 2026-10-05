package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;

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
            List<T> allMatchedExtension;
            try {
                allMatchedExtension = extensionFactory.getAllMatchedExtension(extensionPointClass);
            } catch (QueryException e) {
                // keep List semantics (equals/hashCode follow the resolved elements), but never let
                // logging or a debugger's toString() blow up just because no session is active
                if (ProxyInvocationSupport.isObjectMethod(method) && "toString".equals(method.getName())) {
                    return "AllMatchedProxy[" + extensionPointClass.getName() + "]";
                }
                throw ProxyInvocationSupport.resolveFailed(extensionPointClass, method, e);
            }
            return ProxyInvocationSupport.invokeTarget(allMatchedExtension, method, args);
        }
    }
}
