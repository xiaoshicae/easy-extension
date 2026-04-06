package io.github.xiaoshicae.extension.core.proxy;

import java.util.List;

/**
 * Utility methods shared by proxy factories.
 */
class ProxyUtils {

    /**
     * Build the interfaces array for JDK dynamic proxy creation.
     *
     * @param proxyInterface      the framework proxy interface (e.g., IAbilityProxy)
     * @param extensionPointClasses the extension point interfaces to implement
     * @return array with proxyInterface at index 0, followed by extension point classes
     */
    static Class<?>[] buildInterfaces(Class<?> proxyInterface, List<Class<?>> extensionPointClasses) {
        Class<?>[] interfaces = new Class[extensionPointClasses.size() + 1];
        interfaces[0] = proxyInterface;
        for (int i = 0; i < extensionPointClasses.size(); i++) {
            interfaces[i + 1] = extensionPointClasses.get(i);
        }
        return interfaces;
    }
}
