package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.common.Identifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class FirstMatchedExtensionInvocationHandler<T> implements InvocationHandler {
    private final Class<T> extensionPointClass;
    private final IExtensionFactory extensionFactory;
    private final String toStringMethodName = "toString";

    public FirstMatchedExtensionInvocationHandler(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.extensionFactory = extensionFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        T firstMatchedExtension = extensionFactory.getFirstMatchedExtension(extensionPointClass);
        if (method.getName().equals(toStringMethodName)) {
            return extensionToString(firstMatchedExtension);
        }
        return method.invoke(firstMatchedExtension, args);
    }

    private String extensionToString(T extension) {
        String code = "unknown";
        if (extension instanceof Identifier identifier) {
            code = identifier.code();
        }
        return this + ", with instance code: [" + code + "]";
    }

    @Override
    public String toString() {
        return "JDK dynamic proxy for Extension<" + extensionPointClass.getSimpleName() + ">";
    }
}
