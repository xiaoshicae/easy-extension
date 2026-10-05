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

    /** Describes {@code implementation} as the ability {@code code}, applicable when {@code matcher} accepts the request. */
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

    /** Codes of abilities that must be mounted together with this one. */
    public AbilityDefinition<T> requires(String... abilityCodes) {
        Collections.addAll(requires, abilityCodes);
        return this;
    }

    /** Codes of abilities that must not be mounted together with this one. */
    public AbilityDefinition<T> excludes(String... abilityCodes) {
        Collections.addAll(excludes, abilityCodes);
        return this;
    }

    /** The unique code. */
    public String code() {
        return code;
    }

    /** The matcher deciding whether the ability applies to a request. */
    public Matcher<T> matcher() {
        return matcher;
    }

    /** The implementation object. */
    public Object implementation() {
        return implementation;
    }

    /** The class the implemented extension points are read from. */
    public Class<?> implementationClass() {
        return implementationClass;
    }

    /** Codes of abilities that must be mounted together with this one. */
    public List<String> requires() {
        return Collections.unmodifiableList(requires);
    }

    /** Codes of abilities that must not be mounted together with this one. */
    public List<String> excludes() {
        return Collections.unmodifiableList(excludes);
    }
}
