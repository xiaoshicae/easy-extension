package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.proxy.AbilityProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.BusinessProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.ExtPointDefaultImplProxyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import java.util.Set;

/**
 * Converts instances annotated with {@code @Business}, {@code @Ability} or {@code @ExtensionPointDefaultImplementation}
 * into the proxies the framework registers.
 * <p>
 * Annotations and implemented extension points are read from the instance's class. Each conversion also has an
 * overload taking the <em>target class</em> explicitly, for instances that are themselves proxies created by a
 * container (for example Spring AOP proxies): the proxy class carries none of that metadata, the class behind it does.
 * </p>
 */
public class AnnProxyConvertUtils {
    private static final String ABILITY_PRIORITY_DELIMITER = "::";
    private static final int MAX_ABILITY_ITEMS_CNT = 2;

    public static <T> IExtensionPointGroupDefaultImplementation<T> convertAnnExtensionPointGroupDefaultImplementation(Object instance) throws ProxyException {
        return convertAnnExtensionPointGroupDefaultImplementation(instance, instance.getClass());
    }

    /**
     * Convert a default implementation into the proxy that gets registered.
     *
     * @param <T>         matcher param class
     * @param instance    the default implementation, possibly itself a container-created proxy
     * @param targetClass the class that implements the extension points, when {@code instance} is a proxy of it
     * @return the proxy, which implements every extension point {@code targetClass} implements
     * @throws ProxyException if an extension point is not a public interface or {@code instance} does not implement it
     * @since 4.0
     */
    public static <T> IExtensionPointGroupDefaultImplementation<T> convertAnnExtensionPointGroupDefaultImplementation(Object instance, Class<?> targetClass) throws ProxyException {
        List<Class<?>> implExtPoints = ExtensionPointInterfaces.implementedBy(targetClass);
        ExtPointDefaultImplProxyFactory<T> extPointDefaultImplProxyFactory = new ExtPointDefaultImplProxyFactory<>(instance, targetClass, implExtPoints);
        return extPointDefaultImplProxyFactory.getProxy();
    }

    public static <T> IAbility<T> convertAnnAbilityToProxy(Matcher<T> instance) throws ProxyException {
        return convertAnnAbilityToProxy(instance, instance.getClass());
    }

    /**
     * Convert an ability into the proxy that gets registered.
     *
     * @param <T>         matcher param class
     * @param instance    the ability, possibly itself a container-created proxy
     * @param targetClass the class annotated with {@code @Ability}, when {@code instance} is a proxy of it
     * @return the proxy, which implements every extension point {@code targetClass} implements
     * @throws ProxyException if {@code targetClass} is not annotated with {@code @Ability}, its code is blank, or an
     *                        extension point is not a public interface that {@code instance} implements
     * @since 4.0
     */
    public static <T> IAbility<T> convertAnnAbilityToProxy(Matcher<T> instance, Class<?> targetClass) throws ProxyException {
        Ability ann = targetClass.getAnnotation(Ability.class);
        if (ann == null) {
            throw new ProxyParamException(String.format("ability [%s] must annotated with @Ability", targetClass.getSimpleName()));
        }
        if (ann.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Ability annotate on [%s] should not be blank", targetClass.getSimpleName()));
        }
        List<Class<?>> implExtPoints = ExtensionPointInterfaces.implementedBy(targetClass);
        return new AbilityProxyFactory<>(ann.code(), instance, targetClass, implExtPoints).getProxy();
    }

    public static <T> IBusiness<T> convertAnnBusinessToProxy(Matcher<T> instance) throws ProxyException {
        return convertAnnBusinessToProxy(instance, instance.getClass());
    }

    /**
     * Convert a business into the proxy that gets registered.
     *
     * @param <T>         matcher param class
     * @param instance    the business, possibly itself a container-created proxy
     * @param targetClass the class annotated with {@code @Business}, when {@code instance} is a proxy of it
     * @return the proxy, which implements every extension point {@code targetClass} implements
     * @throws ProxyException if {@code targetClass} is not annotated with {@code @Business}, its code or one of its
     *                        abilities is invalid, or an extension point is not a public interface that
     *                        {@code instance} implements
     * @since 4.0
     */
    public static <T> IBusiness<T> convertAnnBusinessToProxy(Matcher<T> instance, Class<?> targetClass) throws ProxyException {
        Business ann = targetClass.getAnnotation(Business.class);
        if (ann == null) {
            throw new ProxyParamException(String.format("business [%s] must annotated with @Business", targetClass.getSimpleName()));
        }
        if (ann.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Business annotate on [%s] should not be blank", targetClass.getSimpleName()));
        }
        if (Arrays.stream(ann.abilities()).anyMatch(String::isBlank)) {
            throw new ProxyParamException(String.format("abilities of @Business annotate on [%s] can not contain blank ability code", targetClass.getSimpleName()));
        }
        List<Class<?>> implExtPoints = ExtensionPointInterfaces.implementedBy(targetClass);
        List<UsedAbility> usedAbilities = new ArrayList<>();

        int priority = 1;
        Set<String> abilities = new HashSet<>();
        Set<Integer> priorities = new HashSet<>();
        for (String ability : ann.abilities()) {
            UsedAbility usedAbility = resolveUsedAbility(ann.code(), ability, priority);
            if (abilities.contains(usedAbility.code())) {
                throw new ProxyParamException(String.format("abilities of @Business annotate on [%s] contain duplicate code [%s]", targetClass.getSimpleName(), usedAbility.code()));
            }
            if (priorities.contains(usedAbility.priority())) {
                throw new ProxyParamException(String.format("abilities of @Business annotate on [%s] contain duplicate priority [%s]", targetClass.getSimpleName(), usedAbility.priority()));
            }
            abilities.add(usedAbility.code());
            priorities.add(usedAbility.priority());
            usedAbilities.add(usedAbility);
            priority = usedAbility.priority() + 1;
        }
        return new BusinessProxyFactory<>(ann.code(), ann.priority(), usedAbilities, instance, targetClass, implExtPoints).getProxy();
    }

    public static UsedAbility resolveUsedAbility(String business, String ability, int priority) throws ProxyParamException {
        if (ability.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] used ability invalid, code should not be empty", business));
        }

        String[] items = ability.split(ABILITY_PRIORITY_DELIMITER);
        if (items.length > MAX_ABILITY_ITEMS_CNT) {
            throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, format error", business, ability));
        }

        String abilityCode = items[0].trim();
        if (abilityCode.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, code should not be empty", business, ability));
        }

        if (items.length == MAX_ABILITY_ITEMS_CNT) {
            try {
                priority = Integer.parseInt(items[1].trim());
            } catch (NumberFormatException e) {
                throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, priority should be int", business, ability));
            }
        }

        return new UsedAbility(abilityCode, priority);
    }
}
