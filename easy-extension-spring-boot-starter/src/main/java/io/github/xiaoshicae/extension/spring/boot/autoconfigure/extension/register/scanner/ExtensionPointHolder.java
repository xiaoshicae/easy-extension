package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

public class ExtensionPointHolder{
    private final Class<?> extensionPointClass;

    public ExtensionPointHolder(Class<?> extensionPointClass) {
        this.extensionPointClass = extensionPointClass;
    }

    public Class<?> getExtensionPointClass() {
        return extensionPointClass;
    }
}
