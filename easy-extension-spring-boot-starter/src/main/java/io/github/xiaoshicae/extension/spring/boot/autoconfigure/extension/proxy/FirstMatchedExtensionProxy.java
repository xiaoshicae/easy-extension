package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy;

import io.github.xiaoshicae.extension.core.IExtFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class FirstMatchedExtensionProxy<T> implements InvocationHandler {

    private final Class<T> extInterface;

    private final IExtFactory extFactory;

    public FirstMatchedExtensionProxy(Class<T> extInterface, IExtFactory extFactory) {
        this.extInterface = extInterface;
        this.extFactory = extFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        T firstMatchedExtension = this.extFactory.getFirstMatchedExtension(this.extInterface);
        return method.invoke(firstMatchedExtension, args);
    }
}
