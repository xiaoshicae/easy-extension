package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.catalog.AbilityInfo;
import io.github.xiaoshicae.extension.core.catalog.BusinessInfo;
import io.github.xiaoshicae.extension.core.catalog.DefaultImplementationInfo;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.catalog.ExtensionPointInfo;
import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.definition.DefaultImplementationDefinition;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.spi.BusinessResolver;
import io.github.xiaoshicae.extension.core.spi.BusinessSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates all definitions together and builds the immutable context. Every problem is a {@link RegistrationException}.
 */
public final class Assembler {
    private static final Logger logger = LoggerFactory.getLogger(Assembler.class);

    private Assembler() {
    }

    public static <T> ExtensionContext<T> assemble(Set<Class<?>> pointTypes,
                                                   List<DefaultImplementationDefinition> defaultDefinitions,
                                                   List<AbilityDefinition<T>> abilityDefinitions,
                                                   List<BusinessDefinition<T>> businessDefinitions,
                                                   BusinessResolver<T> businessResolver,
                                                   BusinessSelector<T> businessSelector,
                                                   boolean strict,
                                                   Class<?> explicitMatcherParamType) {
        Map<Class<?>, ExtensionPointInfo> points = points(pointTypes);
        Map<Class<?>, Registry.DefaultEntry> defaults = new LinkedHashMap<>();
        List<DefaultImplementationInfo> defaultInfos = defaults(defaultDefinitions, points, defaults);
        Map<String, Registry.AbilityEntry<T>> abilities = abilities(abilityDefinitions, points);
        Map<String, Registry.BusinessEntry<T>> businesses = businesses(businessDefinitions, abilities, points, businessResolver != null);

        for (ExtensionPointInfo point : points.values()) {
            if (!defaults.containsKey(point.type())) {
                defaults.put(point.type(), noOpDefault(point.type()));
            }
        }

        Class<?> matcherParamType = matcherParamType(abilities, businesses, businessResolver != null, explicitMatcherParamType);

        ExtensionCatalog catalog = new ExtensionCatalog(
                List.copyOf(points.values()),
                abilities.values().stream().map(a -> new AbilityInfo(a.code(), a.userClass(), List.copyOf(a.points()), a.requires(), a.excludes())).toList(),
                businesses.values().stream().map(b -> new BusinessInfo(b.code(), b.userClass(), List.copyOf(b.points()), b.mounts())).toList(),
                defaultInfos,
                matcherParamType);

        Registry<T> registry = new Registry<>(Collections.unmodifiableMap(points), Collections.unmodifiableMap(abilities),
                Collections.unmodifiableMap(businesses), Collections.unmodifiableMap(defaults),
                strict, businessResolver, businessSelector, catalog);
        logger.info("[Easy Extension] context built: {} extension points, {} default implementations, {} abilities, {} businesses",
                points.size(), defaultInfos.size(), abilities.size(), businesses.size());
        return new DefaultExtensionContext<>(registry);
    }

    /**
     * The default of an extension point nobody provided one for: possible only when no method needs a return value.
     */
    private static Registry.DefaultEntry noOpDefault(Class<?> point) {
        if (!NoOpDefaults.canBeNoOp(point)) {
            throw new RegistrationException(String.format(
                    "extension point [%s] has no default implementation: add a @DefaultImplementation class for it "
                            + "(only an extension point whose methods all return void gets a no-op default automatically)",
                    point.getName()));
        }
        return new Registry.DefaultEntry(NoOpDefaults.create(point), point, Set.of(point));
    }

    private static Map<Class<?>, ExtensionPointInfo> points(Set<Class<?>> pointTypes) {
        Map<Class<?>, ExtensionPointInfo> points = new LinkedHashMap<>();
        for (Class<?> type : pointTypes) {
            if (type == null || !type.isInterface() || !Modifier.isPublic(type.getModifiers())) {
                throw new RegistrationException(String.format("extension point [%s] should be a public interface",
                        type == null ? null : type.getName()));
            }
            ExtensionPoint annotation = type.getAnnotation(ExtensionPoint.class);
            if (annotation == null) {
                throw new RegistrationException(String.format("extension point [%s] should be annotated with @ExtensionPoint", type.getName()));
            }
            points.put(type, new ExtensionPointInfo(type, annotation.version(), List.of(annotation.scenarios())));
        }
        return points;
    }

    private static List<DefaultImplementationInfo> defaults(List<DefaultImplementationDefinition> definitions,
                                                            Map<Class<?>, ExtensionPointInfo> points,
                                                            Map<Class<?>, Registry.DefaultEntry> defaults) {
        List<DefaultImplementationInfo> infos = new ArrayList<>();
        for (DefaultImplementationDefinition definition : definitions) {
            String label = "default implementation [" + definition.implementationClass().getName() + "]";
            Set<Class<?>> implemented = definition.points().isEmpty()
                    ? derive(definition.implementationClass(), definition.implementation(), points, label)
                    : explicit(definition.points(), definition.implementation(), points, label);
            Registry.DefaultEntry entry = new Registry.DefaultEntry(definition.implementation(), definition.implementationClass(),
                    Collections.unmodifiableSet(implemented));
            for (Class<?> point : implemented) {
                Registry.DefaultEntry previous = defaults.put(point, entry);
                if (previous != null) {
                    throw new RegistrationException(String.format("extension point [%s] has more than one default implementation: [%s] and [%s]",
                            point.getName(), previous.userClass().getName(), entry.userClass().getName()));
                }
            }
            infos.add(new DefaultImplementationInfo(definition.implementationClass(), List.copyOf(implemented)));
        }
        return infos;
    }

    private static <T> Map<String, Registry.AbilityEntry<T>> abilities(List<AbilityDefinition<T>> definitions,
                                                                      Map<Class<?>, ExtensionPointInfo> points) {
        Map<String, Registry.AbilityEntry<T>> abilities = new LinkedHashMap<>();
        for (AbilityDefinition<T> definition : definitions) {
            String label = "ability [" + definition.code() + "]";
            if (definition.matcher() == null) {
                throw new RegistrationException(label + " should implement Matcher");
            }
            Set<Class<?>> implemented = derive(definition.implementationClass(), definition.implementation(), points, label);
            Registry.AbilityEntry<T> entry = new Registry.AbilityEntry<>(definition.code(), definition.matcher(), definition.implementation(),
                    definition.implementationClass(), Collections.unmodifiableSet(implemented),
                    List.copyOf(definition.requires()), List.copyOf(definition.excludes()));
            if (abilities.putIfAbsent(definition.code(), entry) != null) {
                throw new RegistrationException(String.format("ability [%s] already registered", definition.code()));
            }
        }
        for (Registry.AbilityEntry<T> ability : abilities.values()) {
            for (String code : ability.requires()) {
                requireAbility(abilities, code, "ability [" + ability.code() + "] requires");
            }
            for (String code : ability.excludes()) {
                requireAbility(abilities, code, "ability [" + ability.code() + "] excludes");
            }
        }
        for (Registry.AbilityEntry<T> ability : abilities.values()) {
            if (ability.requires().contains(ability.code()) || ability.excludes().contains(ability.code())) {
                throw new RegistrationException(String.format("ability [%s] cannot require or exclude itself", ability.code()));
            }
            for (String required : ability.requires()) {
                if (ability.excludes().contains(required)) {
                    throw new RegistrationException(String.format("ability [%s] both requires and excludes ability [%s]", ability.code(), required));
                }
                if (abilities.get(required).excludes().contains(ability.code())) {
                    throw new RegistrationException(String.format("ability [%s] requires ability [%s], which excludes it", ability.code(), required));
                }
            }
        }
        return abilities;
    }

    private static <T> Map<String, Registry.BusinessEntry<T>> businesses(List<BusinessDefinition<T>> definitions,
                                                                        Map<String, Registry.AbilityEntry<T>> abilities,
                                                                        Map<Class<?>, ExtensionPointInfo> points,
                                                                        boolean hasBusinessResolver) {
        Map<String, Registry.BusinessEntry<T>> businesses = new LinkedHashMap<>();
        for (BusinessDefinition<T> definition : definitions) {
            String code = definition.code();
            String label = "business [" + code + "]";
            if (definition.matcher() == null && !hasBusinessResolver) {
                throw new RegistrationException(label + " should implement Matcher (or configure a BusinessResolver)");
            }
            if (abilities.containsKey(code)) {
                throw new RegistrationException(String.format("code [%s] is used by both an ability and a business", code));
            }
            Set<Class<?>> implemented = derive(definition.implementationClass(), definition.implementation(), points, label, true);
            List<MountInfo> mounts = mounts(definition, abilities);
            Registry.BusinessEntry<T> entry = new Registry.BusinessEntry<>(code, definition.matcher(), definition.implementation(),
                    definition.implementationClass(), Collections.unmodifiableSet(implemented), mounts);
            if (businesses.putIfAbsent(code, entry) != null) {
                throw new RegistrationException(String.format("business [%s] already registered", code));
            }
            checkAbilityConstraints(code, mounts, abilities);
        }
        return businesses;
    }

    private static <T> List<MountInfo> mounts(BusinessDefinition<T> definition, Map<String, Registry.AbilityEntry<T>> abilities) {
        List<MountInfo> mounts = new ArrayList<>();
        Set<String> mountedCodes = new HashSet<>();
        boolean hasSelf = false;
        for (MountInfo mount : definition.mounts()) {
            if (mount.isSelf()) {
                if (hasSelf) {
                    throw new RegistrationException(String.format("business [%s] places itself (Self) more than once in abilities", definition.code()));
                }
                hasSelf = true;
            } else {
                if (!abilities.containsKey(mount.abilityCode())) {
                    throw new RegistrationException(String.format("business [%s] uses ability [%s] which is not registered",
                            definition.code(), mount.abilityCode()));
                }
                if (!mountedCodes.add(mount.abilityCode())) {
                    throw new RegistrationException(String.format("business [%s] uses ability [%s] more than once",
                            definition.code(), mount.abilityCode()));
                }
            }
            mounts.add(mount);
        }
        if (!hasSelf) {
            mounts.add(0, MountInfo.self());
        }
        return Collections.unmodifiableList(mounts);
    }

    private static <T> void checkAbilityConstraints(String businessCode, List<MountInfo> mounts, Map<String, Registry.AbilityEntry<T>> abilities) {
        Set<String> mounted = new LinkedHashSet<>();
        for (MountInfo mount : mounts) {
            if (!mount.isSelf()) {
                mounted.add(mount.abilityCode());
            }
        }
        for (String abilityCode : mounted) {
            Registry.AbilityEntry<T> ability = abilities.get(abilityCode);
            for (String required : ability.requires()) {
                if (!mounted.contains(required)) {
                    throw new RegistrationException(String.format(
                            "business [%s] mounts ability [%s] which requires ability [%s], but [%s] is not mounted",
                            businessCode, abilityCode, required, required));
                }
            }
            for (String excluded : ability.excludes()) {
                if (mounted.contains(excluded)) {
                    throw new RegistrationException(String.format(
                            "business [%s] mounts ability [%s] which excludes ability [%s], but both are mounted",
                            businessCode, abilityCode, excluded));
                }
            }
        }
    }

    private static <T> void requireAbility(Map<String, Registry.AbilityEntry<T>> abilities, String code, String owner) {
        if (!abilities.containsKey(code)) {
            throw new RegistrationException(String.format("%s ability [%s] which is not registered", owner, code));
        }
    }

    /**
     * Extension points a default was explicitly restricted to; all must be registered and implemented.
     */
    private static Set<Class<?>> explicit(Set<Class<?>> requested, Object implementation,
                                          Map<Class<?>, ExtensionPointInfo> registered, String label) {
        for (Class<?> point : requested) {
            if (!registered.containsKey(point)) {
                throw new RegistrationException(String.format("extension point [%s] of %s is not registered", point.getName(), label));
            }
            if (!point.isInstance(implementation)) {
                throw new RegistrationException(String.format("%s is not an instance of extension point [%s]", label, point.getName()));
            }
        }
        return new LinkedHashSet<>(requested);
    }

    /**
     * The extension points an implementation provides, derived from its class hierarchy; all must be registered.
     */
    private static Set<Class<?>> derive(Class<?> userClass, Object implementation, Map<Class<?>, ExtensionPointInfo> registered, String label) {
        return derive(userClass, implementation, registered, label, false);
    }

    /**
     * @param mayBeEmpty whether implementing no extension point at all is fine: a business that only identifies a
     *                   request and lets every extension point fall through to its default
     */
    private static Set<Class<?>> derive(Class<?> userClass, Object implementation, Map<Class<?>, ExtensionPointInfo> registered,
                                        String label, boolean mayBeEmpty) {
        // not checked against userClass: a JDK dynamic proxy is not an instance of its target class
        Set<Class<?>> implemented = Introspector.extensionPointsOf(userClass);
        if (implemented.isEmpty() && !mayBeEmpty) {
            throw new RegistrationException(label + " does not implement any extension point");
        }
        for (Class<?> point : implemented) {
            if (!registered.containsKey(point)) {
                throw new RegistrationException(String.format(
                        "extension point [%s] implemented by %s is not registered: register it, or add its package to the scan",
                        point.getName(), label));
            }
            if (!point.isInstance(implementation)) {
                throw new RegistrationException(String.format("%s is not an instance of extension point [%s]", label, point.getName()));
            }
        }
        return implemented;
    }

    /**
     * The request parameter type: the configured one (every matcher must accept it), else the most specific type
     * the matchers declare (every other matcher must accept it). Businesses ignore their matcher when a
     * {@code BusinessResolver} routes requests.
     */
    private static <T> Class<?> matcherParamType(Map<String, Registry.AbilityEntry<T>> abilities,
                                                 Map<String, Registry.BusinessEntry<T>> businesses,
                                                 boolean hasBusinessResolver, Class<?> explicitType) {
        Map<String, Class<?>> declared = new LinkedHashMap<>();
        for (Registry.AbilityEntry<T> ability : abilities.values()) {
            addIfKnown(declared, "ability [" + ability.code() + "]", ability.userClass());
        }
        if (!hasBusinessResolver) {
            for (Registry.BusinessEntry<T> business : businesses.values()) {
                if (business.matcher() != null) {
                    addIfKnown(declared, "business [" + business.code() + "]", business.userClass());
                }
            }
        }

        if (explicitType != null) {
            declared.forEach((owner, type) -> {
                if (!type.isAssignableFrom(explicitType)) {
                    throw new RegistrationException(String.format("%s matches on [%s], which does not accept the configured matcher param type [%s]",
                            owner, type.getName(), explicitType.getName()));
                }
            });
            return explicitType;
        }

        Class<?> mostSpecific = null;
        for (Class<?> type : declared.values()) {
            if (mostSpecific == null || mostSpecific.isAssignableFrom(type)) {
                mostSpecific = type;
            }
        }
        for (Class<?> type : declared.values()) {
            if (!type.isAssignableFrom(mostSpecific)) {
                throw new RegistrationException("abilities and businesses match on different parameter types: "
                        + new ArrayList<>(new LinkedHashSet<>(declared.values())) + ", they must share one (or set matcherParamType explicitly)");
            }
        }
        return mostSpecific;
    }

    private static void addIfKnown(Map<String, Class<?>> declared, String owner, Class<?> userClass) {
        Class<?> type = MatcherTypes.matcherParamTypeOf(userClass);
        if (type != null) {
            declared.put(owner, type);
        }
    }
}
