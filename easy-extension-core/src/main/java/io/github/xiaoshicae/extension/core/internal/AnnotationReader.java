package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.Self;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.definition.DefaultImplementationDefinition;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

/**
 * Turns annotated objects into definitions.
 */
public final class AnnotationReader {

    private AnnotationReader() {
    }

    @SuppressWarnings("unchecked")
    public static <T> AbilityDefinition<T> ability(Object implementation, Class<?> userClass) {
        Ability annotation = userClass.getAnnotation(Ability.class);
        if (annotation == null) {
            throw new RegistrationException(String.format("ability [%s] must be annotated with @Ability", userClass.getName()));
        }
        Matcher<T> matcher = implementation instanceof Matcher<?> m ? (Matcher<T>) m : null;
        AbilityDefinition<T> definition = AbilityDefinition.of(codeOf(annotation.code(), userClass), matcher, implementation)
                .implementationClass(userClass);
        for (Class<?> required : annotation.requires()) {
            definition.requires(abilityCodeOf(required));
        }
        for (Class<?> excluded : annotation.excludes()) {
            definition.excludes(abilityCodeOf(excluded));
        }
        return definition;
    }

    @SuppressWarnings("unchecked")
    public static <T> BusinessDefinition<T> business(Object implementation, Class<?> userClass) {
        Business annotation = userClass.getAnnotation(Business.class);
        if (annotation == null) {
            throw new RegistrationException(String.format("business [%s] must be annotated with @Business", userClass.getName()));
        }
        Matcher<T> matcher = implementation instanceof Matcher<?> m ? (Matcher<T>) m : null;
        BusinessDefinition<T> definition = BusinessDefinition.of(codeOf(annotation.code(), userClass), matcher, implementation)
                .implementationClass(userClass);
        for (Class<?> use : annotation.abilities()) {
            if (use == Self.class) {
                definition.self();
            } else {
                definition.ability(abilityCodeOf(use));
            }
        }
        return definition;
    }

    public static DefaultImplementationDefinition defaultImplementation(Object implementation, Class<?> userClass) {
        if (!userClass.isAnnotationPresent(DefaultImplementation.class)) {
            throw new RegistrationException(String.format(
                    "default implementation [%s] must be annotated with @DefaultImplementation", userClass.getName()));
        }
        return DefaultImplementationDefinition.of(implementation).implementationClass(userClass);
    }

    /**
     * Code of the ability a {@code abilities}/{@code requires}/{@code excludes} entry refers to.
     */
    static String abilityCodeOf(Class<?> abilityClass) {
        Ability annotation = abilityClass.getAnnotation(Ability.class);
        if (annotation == null) {
            throw new RegistrationException(String.format("[%s] is referenced as an ability but is not annotated with @Ability",
                    abilityClass.getName()));
        }
        return codeOf(annotation.code(), abilityClass);
    }

    private static String codeOf(String declared, Class<?> userClass) {
        return declared.isBlank() ? userClass.getName() : declared;
    }
}
