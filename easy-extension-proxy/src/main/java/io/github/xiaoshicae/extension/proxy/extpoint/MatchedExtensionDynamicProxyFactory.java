package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.IExtensionFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class MatchedExtensionDynamicProxyFactory<T> {
    private final Class<T> extensionPointClass;
    private final Class<?> extensionPointListClass = ArrayList.class;
    private IExtensionFactory extensionFactory;

    public MatchedExtensionDynamicProxyFactory(Class<T> extensionPointClass) {
        this.extensionPointClass = extensionPointClass;
    }

    @SuppressWarnings("unchecked")
    public T newFirstMatchedInstance() {
        return (T) Proxy.newProxyInstance(extensionPointClass.getClassLoader(), new Class[]{extensionPointClass}, new FirstMatchedExtensionInvocationHandler<>(extensionPointClass, extensionFactory));
    }

    @SuppressWarnings("unchecked")
    public List<T> newAllMatchedInstance() {
        return (List<T>) Proxy.newProxyInstance(extensionPointListClass.getClassLoader(), extensionPointListClass.getInterfaces(), new AllMatchedExtensionInvocationHandler<>(extensionPointClass, extensionFactory));
    }

    public IExtensionFactory getExtensionFactory() {
        return extensionFactory;
    }

    public void setExtensionFactory(IExtensionFactory extensionFactory) {
        this.extensionFactory = extensionFactory;
    }
}
