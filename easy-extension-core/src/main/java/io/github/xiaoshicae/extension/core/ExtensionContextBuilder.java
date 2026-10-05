package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.definition.DefaultImplementationDefinition;
import io.github.xiaoshicae.extension.core.internal.Assembler;
import io.github.xiaoshicae.extension.core.internal.AnnotationReader;
import io.github.xiaoshicae.extension.core.spi.BusinessResolver;
import io.github.xiaoshicae.extension.core.spi.BusinessSelector;
import io.github.xiaoshicae.extension.core.spi.OrderedCodeBusinessSelector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Collects everything to register, then validates it all at once in {@link #build()}. Not thread-safe.
 * <p>
 * Annotated objects ({@code ability(Object)} etc.) are read from the annotations of their class; pass the user's
 * class explicitly when the object is a proxy of it (the extension points are derived from that class).
 * </p>
 *
 * @param <T> matcher param type
 */
public final class ExtensionContextBuilder<T> {
    private final Set<Class<?>> extensionPoints = new LinkedHashSet<>();
    private final List<DefaultImplementationDefinition> defaultImplementations = new ArrayList<>();
    private final List<AbilityDefinition<T>> abilities = new ArrayList<>();
    private final List<BusinessDefinition<T>> businesses = new ArrayList<>();
    private BusinessResolver<T> businessResolver;
    private BusinessSelector<T> businessSelector = new OrderedCodeBusinessSelector<>(List.of());
    private boolean strict = true;
    private Class<?> matcherParamType;

    ExtensionContextBuilder() {
    }

    /**
     * Registers extension points: public interfaces annotated with {@code @ExtensionPoint}.
     */
    public ExtensionContextBuilder<T> extensionPoint(Class<?>... types) {
        extensionPoints.addAll(Arrays.asList(types));
        return this;
    }

    public ExtensionContextBuilder<T> defaultImplementation(Object implementation) {
        return defaultImplementation(implementation, requireImplementation(implementation).getClass());
    }

    public ExtensionContextBuilder<T> defaultImplementation(Object implementation, Class<?> userClass) {
        return defaultImplementation(AnnotationReader.defaultImplementation(implementation, userClass));
    }

    /**
     * Registers an implementation of a single extension point as its default, without an annotated class,
     * e.g. a lambda for a single-method extension point.
     */
    public <E> ExtensionContextBuilder<T> defaultImplementationFor(Class<E> point, E implementation) {
        Objects.requireNonNull(point, "point");
        return defaultImplementation(DefaultImplementationDefinition.of(requireImplementation(implementation)));
    }

    public ExtensionContextBuilder<T> defaultImplementation(DefaultImplementationDefinition definition) {
        defaultImplementations.add(Objects.requireNonNull(definition, "definition"));
        return this;
    }

    public ExtensionContextBuilder<T> ability(Object implementation) {
        return ability(implementation, requireImplementation(implementation).getClass());
    }

    public ExtensionContextBuilder<T> ability(Object implementation, Class<?> userClass) {
        return ability(AnnotationReader.<T>ability(implementation, userClass));
    }

    public ExtensionContextBuilder<T> ability(AbilityDefinition<T> definition) {
        abilities.add(Objects.requireNonNull(definition, "definition"));
        return this;
    }

    public ExtensionContextBuilder<T> business(Object implementation) {
        return business(implementation, requireImplementation(implementation).getClass());
    }

    public ExtensionContextBuilder<T> business(Object implementation, Class<?> userClass) {
        return business(AnnotationReader.<T>business(implementation, userClass));
    }

    public ExtensionContextBuilder<T> business(BusinessDefinition<T> definition) {
        businesses.add(Objects.requireNonNull(definition, "definition"));
        return this;
    }

    /**
     * Routes requests to businesses by code instead of asking each business to match.
     */
    public ExtensionContextBuilder<T> businessResolver(BusinessResolver<T> businessResolver) {
        this.businessResolver = businessResolver;
        return this;
    }

    /**
     * Chooses among several matching businesses; only used when not {@link #strict(boolean) strict}.
     */
    public ExtensionContextBuilder<T> businessSelector(BusinessSelector<T> businessSelector) {
        this.businessSelector = Objects.requireNonNull(businessSelector, "businessSelector");
        return this;
    }

    /**
     * Strict (the default): exactly one business must match every request. Not strict: no match means only default
     * implementations answer, several matches are settled by the {@link #businessSelector(BusinessSelector) selector}.
     */
    public ExtensionContextBuilder<T> strict(boolean strict) {
        this.strict = strict;
        return this;
    }

    /**
     * The request parameter type; derived from the {@code Matcher<T>} implementations when not given.
     */
    public ExtensionContextBuilder<T> matcherParamType(Class<?> matcherParamType) {
        this.matcherParamType = matcherParamType;
        return this;
    }

    private static Object requireImplementation(Object implementation) {
        if (implementation == null) {
            throw new RegistrationException("implementation should not be null");
        }
        return implementation;
    }

    /**
     * Validates everything and builds the immutable context.
     *
     * @throws io.github.xiaoshicae.extension.core.exception.RegistrationException if the setup is invalid
     */
    public ExtensionContext<T> build() {
        return Assembler.assemble(extensionPoints, defaultImplementations, abilities, businesses,
                businessResolver, businessSelector, strict, matcherParamType);
    }
}
