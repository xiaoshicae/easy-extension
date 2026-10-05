package io.github.xiaoshicae.extension.core.definition;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes a default implementation without annotations, for programmatic assembly.
 */
public final class DefaultImplementationDefinition {
    private final Object implementation;
    private Class<?> implementationClass;
    private Set<Class<?>> points = Set.of();

    private DefaultImplementationDefinition(Object implementation) {
        this.implementation = implementation;
        this.implementationClass = implementation.getClass();
    }

    /** Describes {@code implementation} as a default implementation. */
    public static DefaultImplementationDefinition of(Object implementation) {
        return new DefaultImplementationDefinition(Objects.requireNonNull(implementation, "implementation"));
    }

    /** Overrides the class the implemented extension points are read from (default: the object's class). */
    public DefaultImplementationDefinition implementationClass(Class<?> implementationClass) {
        this.implementationClass = Objects.requireNonNull(implementationClass, "implementationClass");
        return this;
    }

    /**
     * Restricts the default to these extension points instead of every extension point the implementation's
     * class hierarchy provides.
     */
    public DefaultImplementationDefinition forPoints(Class<?>... points) {
        Set<Class<?>> restricted = new LinkedHashSet<>(Arrays.asList(points));
        if (restricted.isEmpty() || restricted.contains(null)) {
            throw new IllegalArgumentException("points should not be empty or contain null");
        }
        this.points = Set.copyOf(restricted);
        return this;
    }

    /**
     * The explicit extension points of this default, empty when they are derived from the implementation's class.
     */
    public Set<Class<?>> points() {
        return points;
    }

    /**
     * The default implementation object.
     */
    public Object implementation() {
        return implementation;
    }

    /**
     * The class the framework reads the implemented extension points from.
     */
    public Class<?> implementationClass() {
        return implementationClass;
    }
}
