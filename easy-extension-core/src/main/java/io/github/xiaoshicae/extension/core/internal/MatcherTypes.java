package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds the {@code T} of the {@code Matcher<T>} a class implements, through generic superclasses and interfaces.
 */
final class MatcherTypes {

    private MatcherTypes() {
    }

    /**
     * @return the parameter class, or {@code null} if it is not statically known (raw type, type variable, lambda)
     */
    static Class<?> matcherParamTypeOf(Class<?> type) {
        return find(type, new HashMap<>());
    }

    private static Class<?> find(Type type, Map<TypeVariable<?>, Type> bindings) {
        Class<?> raw;
        Map<TypeVariable<?>, Type> next = new HashMap<>(bindings);
        if (type instanceof ParameterizedType parameterized) {
            raw = (Class<?>) parameterized.getRawType();
            TypeVariable<?>[] variables = raw.getTypeParameters();
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int i = 0; i < variables.length; i++) {
                next.put(variables[i], resolve(arguments[i], bindings));
            }
            if (raw == Matcher.class) {
                return toClass(next.get(variables[0]));
            }
        } else if (type instanceof Class<?> clazz) {
            raw = clazz;
        } else {
            return null;
        }
        for (Type parent : raw.getGenericInterfaces()) {
            Class<?> found = find(parent, next);
            if (found != null) {
                return found;
            }
        }
        Type superclass = raw.getGenericSuperclass();
        return superclass == null ? null : find(superclass, next);
    }

    private static Type resolve(Type type, Map<TypeVariable<?>, Type> bindings) {
        return type instanceof TypeVariable<?> variable ? bindings.getOrDefault(variable, variable) : type;
    }

    private static Class<?> toClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized) {
            return (Class<?>) parameterized.getRawType();
        }
        if (type instanceof WildcardType wildcard) {
            return toClass(wildcard.getUpperBounds()[0]);
        }
        if (type instanceof TypeVariable<?> variable) {
            // not bound by the implementing class: fall back to the declared bound (Object if none)
            return toClass(variable.getBounds()[0]);
        }
        return null;
    }
}
