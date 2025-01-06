package io.github.xiaoshicae.extension.proxy.util;

import java.lang.reflect.Method;

public class CGLibProxyDefinition<T> {
    private Class<?> superClass;
    private Class<?>[] implInterfaces;
    private T instance;
    private Method[] mustInvokeSuperMethods;

    public CGLibProxyDefinition() {
    }

    public Class<?> getSuperClass() {
        return superClass;
    }

    public void setSuperClass(Class<?> superClass) {
        this.superClass = superClass;
    }

    public T getInstance() {
        return instance;
    }

    public void setInstance(T instance) {
        this.instance = instance;
    }

    public Class<?>[] getImplInterfaces() {
        return implInterfaces;
    }

    public void setImplInterfaces(Class<?> ... implInterfaces) {
        this.implInterfaces = implInterfaces;
    }

    public Method[] getMustInvokeSuperMethods() {
        return mustInvokeSuperMethods;
    }

    public void setMustInvokeSuperMethods(Method ...mustInvokeSuperMethods) {
        this.mustInvokeSuperMethods = mustInvokeSuperMethods;
    }
}
