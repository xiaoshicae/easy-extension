package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.common.Identifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class AllMatchedExtensionInvocationHandler<T> implements InvocationHandler {
    private final Class<T> extensionPointClass;
    private final IExtensionFactory extensionFactory;
    private final String toStringMethodName = "toString";

    public AllMatchedExtensionInvocationHandler(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.extensionFactory = extensionFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<T> allMatchedExtension = extensionFactory.getAllMatchedExtension(extensionPointClass);
        if (method.getName().equals(toStringMethodName)) {
            return extensionListToString(allMatchedExtension);
        }
        return method.invoke(allMatchedExtension, args);
    }

    private String extensionListToString(List<T> extensionList) {
        List<String> codes = extensionList.stream().filter(e -> e instanceof Identifier).map(e -> ((Identifier) e).code()).toList();
        return this + ", with instance codes: [" + String.join(", ", codes) + "]";
    }

    @Override
    public String toString() {
        return "JDK dynamic proxy for Extension<List<" + extensionPointClass.getSimpleName() + ">>";
    }
}
