package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the extension points an implementation class implements.
 */
final class ExtensionPointInterfaces {

    private ExtensionPointInterfaces() {
    }

    /**
     * Interfaces annotated with {@link ExtensionPoint} that {@code type} implements: declared by the class itself,
     * or inherited from a superclass or a super-interface.
     * <p>
     * The interfaces the class declares itself come first, in declaration order; they are all taken, so that a
     * problem with one of them (it is not public, say) is reported. An inherited one is only taken if it is public:
     * a package-private interface cannot be proxied, and releases before 4.0, which looked at the declared
     * interfaces only, never saw it.
     * </p>
     */
    static List<Class<?>> implementedBy(Class<?> type) {
        Set<Class<?>> found = new LinkedHashSet<>();
        for (Class<?> declared : type.getInterfaces()) {
            if (declared.isAnnotationPresent(ExtensionPoint.class)) {
                found.add(declared);
            }
        }
        for (Class<?> declared : type.getInterfaces()) {
            collectInherited(declared.getInterfaces(), found);
        }
        for (Class<?> parent = type.getSuperclass(); parent != null && parent != Object.class; parent = parent.getSuperclass()) {
            collectInherited(parent.getInterfaces(), found);
        }
        return List.copyOf(found);
    }

    private static void collectInherited(Class<?>[] interfaces, Set<Class<?>> found) {
        for (Class<?> candidate : interfaces) {
            if (candidate.isAnnotationPresent(ExtensionPoint.class) && Modifier.isPublic(candidate.getModifiers())) {
                found.add(candidate);
            }
            collectInherited(candidate.getInterfaces(), found);
        }
    }
}
