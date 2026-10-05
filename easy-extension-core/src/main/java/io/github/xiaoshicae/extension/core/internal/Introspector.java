package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Finds the extension points a class implements, wherever they sit in its type hierarchy.
 */
final class Introspector {

    private Introspector() {
    }

    /**
     * Interfaces annotated with {@link ExtensionPoint} among the interfaces and superclasses of {@code type},
     * recursively, the class's own interfaces first.
     */
    static Set<Class<?>> extensionPointsOf(Class<?> type) {
        Set<Class<?>> result = new LinkedHashSet<>();
        collect(type, result, new HashSet<>());
        return result;
    }

    private static void collect(Class<?> type, Set<Class<?>> result, Set<Class<?>> visited) {
        if (type == null || type == Object.class || !visited.add(type)) {
            return;
        }
        if (type.isInterface() && type.isAnnotationPresent(ExtensionPoint.class)) {
            result.add(type);
        }
        for (Class<?> parent : type.getInterfaces()) {
            collect(parent, result, visited);
        }
        collect(type.getSuperclass(), result, visited);
    }
}
