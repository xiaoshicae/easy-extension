package io.github.xiaoshicae.extension.core.definition;

import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes an ability without annotations, for programmatic assembly. Codes in {@link #requires} and
 * {@link #excludes} refer to other abilities by code.
 *
 * @param <T> matcher param type
 */
public final class AbilityDefinition<T> {
    private final String code;
    private final Matcher<T> matcher;
    private final Object implementation;
    private Class<?> implementationClass;
    private final List<String> requires = new ArrayList<>();
    private final List<String> excludes = new ArrayList<>();

    private AbilityDefinition(String code, Matcher<T> matcher, Object implementation) {
        this.code = code;
        this.matcher = matcher;
        this.implementation = implementation;
        this.implementationClass = implementation.getClass();
    }

    public static <T> AbilityDefinition<T> of(String code, Matcher<T> matcher, Object implementation) {
        if (code == null || code.isBlank()) {
            throw new RegistrationException("ability code should not be blank");
        }
        Objects.requireNonNull(implementation, "implementation");
        return new AbilityDefinition<>(code, matcher, implementation);
    }

    /**
     * The user's class, when {@code implementation} is a proxy of it (extension points are derived from this class).
     */
    public AbilityDefinition<T> implementationClass(Class<?> implementationClass) {
        this.implementationClass = Objects.requireNonNull(implementationClass, "implementationClass");
        return this;
    }

    public AbilityDefinition<T> requires(String... abilityCodes) {
        Collections.addAll(requires, abilityCodes);
        return this;
    }

    public AbilityDefinition<T> excludes(String... abilityCodes) {
        Collections.addAll(excludes, abilityCodes);
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

    public List<String> requires() {
        return Collections.unmodifiableList(requires);
    }

    public List<String> excludes() {
        return Collections.unmodifiableList(excludes);
    }
}
