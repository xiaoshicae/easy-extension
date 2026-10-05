package io.github.xiaoshicae.extension.core.definition;

import java.util.Objects;

/**
 * Describes a default implementation without annotations, for programmatic assembly.
 */
public final class DefaultImplementationDefinition {
    private final Object implementation;
    private Class<?> implementationClass;

    private DefaultImplementationDefinition(Object implementation) {
        this.implementation = implementation;
        this.implementationClass = implementation.getClass();
    }

    public static DefaultImplementationDefinition of(Object implementation) {
        return new DefaultImplementationDefinition(Objects.requireNonNull(implementation, "implementation"));
    }

    public DefaultImplementationDefinition implementationClass(Class<?> implementationClass) {
        this.implementationClass = Objects.requireNonNull(implementationClass, "implementationClass");
        return this;
    }

    public Object implementation() {
        return implementation;
    }

    public Class<?> implementationClass() {
        return implementationClass;
    }
}
