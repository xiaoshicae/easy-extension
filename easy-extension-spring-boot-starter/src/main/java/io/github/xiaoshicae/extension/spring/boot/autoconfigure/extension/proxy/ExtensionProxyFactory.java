package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy;

import io.github.xiaoshicae.extension.core.IExtFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class ExtensionProxyFactory<T> {
    private Class<T> extInterface;

    private final Class<ArrayList<T>> extListInterface;

    private IExtFactory extFactory;

    public ExtensionProxyFactory(Class<T> extInterface) {
        this.extInterface = extInterface;
        this.extListInterface = (Class<ArrayList<T>>) (new ArrayList<T>()).getClass();
    }

    public T newFirstMatchedInstance() {
        return (T) Proxy.newProxyInstance(this.extInterface.getClassLoader(), new Class[]{this.extInterface}, new FirstMatchedExtensionProxy(this.extInterface, this.extFactory));
    }

    public List<T> newAllMatchedInstance() {
        return (List<T>) Proxy.newProxyInstance(this.extListInterface.getClassLoader(), this.extListInterface.getInterfaces(), new AllMatchedExtensionProxy(this.extInterface, this.extFactory));
    }

    public IExtFactory getExtFactory() {
        return this.extFactory;
    }

    public void setExtFactory(IExtFactory extFactory) {
        this.extFactory = extFactory;
    }

    public Class<T> getExtInterface() {
        return this.extInterface;
    }

    public void setExtInterface(Class<T> extInterface) {
        this.extInterface = extInterface;
    }
}
