package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

/**
 * Bean that records a scanned {@code @ExtensionPoint} interface, so the context can be built from the container.
 */
public class ExtensionPointHolder {
    private final Class<?> extensionPointClass;

    /**
     * Creates the holder.
     *
     * @param extensionPointClass the extension point interface
     */
    public ExtensionPointHolder(Class<?> extensionPointClass) {
        this.extensionPointClass = extensionPointClass;
    }

    /**
     * The extension point interface.
     */
    public Class<?> getExtensionPointClass() {
        return extensionPointClass;
    }
}
