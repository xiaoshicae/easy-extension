package io.github.xiaoshicae.extension.core.definition;

import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes a business without annotations, for programmatic assembly. {@link #uses} and {@link #self} append to the
 * precedence order; without {@code self()} the business comes first.
 *
 * @param <T> matcher param type
 */
public final class BusinessDefinition<T> {
    private final String code;
    private final Matcher<T> matcher;
    private final Object implementation;
    private Class<?> implementationClass;
    private final List<MountInfo> mounts = new ArrayList<>();

    private BusinessDefinition(String code, Matcher<T> matcher, Object implementation) {
        this.code = code;
        this.matcher = matcher;
        this.implementation = implementation;
        this.implementationClass = implementation.getClass();
    }

    /**
     * @param matcher {@code null} when the context uses a {@code BusinessResolver}
     */
    public static <T> BusinessDefinition<T> of(String code, Matcher<T> matcher, Object implementation) {
        if (code == null || code.isBlank()) {
            throw new RegistrationException("business code should not be blank");
        }
        Objects.requireNonNull(implementation, "implementation");
        return new BusinessDefinition<>(code, matcher, implementation);
    }

    public BusinessDefinition<T> implementationClass(Class<?> implementationClass) {
        this.implementationClass = Objects.requireNonNull(implementationClass, "implementationClass");
        return this;
    }

    /**
     * Mounts an ability, after everything added so far.
     */
    public BusinessDefinition<T> uses(String abilityCode) {
        mounts.add(MountInfo.ability(abilityCode));
        return this;
    }

    /**
     * Places the business's own implementation here in the precedence order.
     */
    public BusinessDefinition<T> self() {
        mounts.add(MountInfo.self());
        return this;
    }

    public String code() {
        return code;
    }

    public Matcher<T> matcher() {
        return matcher;
    }

    public Object implementation() {
        return implementation;
    }

    public Class<?> implementationClass() {
        return implementationClass;
    }

    public List<MountInfo> mounts() {
        return Collections.unmodifiableList(mounts);
    }
}
