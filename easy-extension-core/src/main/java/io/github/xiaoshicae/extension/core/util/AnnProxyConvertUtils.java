package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.proxy.AbilityProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.BusinessProxyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AnnProxyConvertUtils {
    private static final String abilityPriorityDelimiter = "::";
    private static final Integer maxAbilityItemsCnt = 2;

    public static <T> IAbility<T> convertAnnAbilityToProxy(Matcher<T> instance) throws ProxyException {
        Ability ann = instance.getClass().getAnnotation(Ability.class);
        if (Objects.isNull(ann)) {
            throw new ProxyParamException(String.format("ability [%s] must annotated with @Ability", instance.getClass().getSimpleName()));
        }
        if (ann.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Ability annotate on [%s] should not be blank", instance.getClass().getSimpleName()));
        }
        List<Class<?>> implExtPoints = Arrays.stream(instance.getClass().getInterfaces()).filter(i -> i.isAnnotationPresent(ExtensionPoint.class)).toList();
        return new AbilityProxyFactory<>(ann.code(), instance, implExtPoints).getProxy();
    }

    public static <T> IBusiness<T> convertAnnBusinessToProxy(Matcher<T> instance) throws ProxyException {
        Business ann = instance.getClass().getAnnotation(Business.class);
        if (Objects.isNull(ann)) {
            throw new ProxyParamException(String.format("business [%s] must annotated with @Business", instance.getClass().getSimpleName()));
        }
        if (ann.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Business annotate on [%s] should not be blank", instance.getClass().getSimpleName()));
        }
        if (Arrays.stream(ann.abilities()).anyMatch(String::isBlank)) {
            throw new ProxyParamException(String.format("abilities of @Business annotate on [%s] can not contain blank ability code", instance.getClass().getSimpleName()));
        }
        List<Class<?>> implExtPoints = Arrays.stream(instance.getClass().getInterfaces()).filter(i -> i.isAnnotationPresent(ExtensionPoint.class)).toList();
        List<UsedAbility> usedAbilities = new ArrayList<>();

        int priority = 1;
        Set<String> abilities = new HashSet<>();
        Set<Integer> priorities = new HashSet<>();
        for (String ability : ann.abilities()) {
            UsedAbility usedAbility = resolveUsedAbility(ann.code(), ability, priority);
            if (abilities.contains(usedAbility.code())) {
                throw new IllegalArgumentException(String.format("abilities of @Business annotate on [%s] contain duplicate code [%s]", instance.getClass().getSimpleName(), usedAbility.code()));
            }
            if (priorities.contains(usedAbility.priority())) {
                throw new IllegalArgumentException(String.format("abilities of @Business annotate on [%s] contain duplicate priority [%s]", instance.getClass().getSimpleName(), usedAbility.priority()));
            }
            abilities.add(usedAbility.code());
            priorities.add(usedAbility.priority());
            usedAbilities.add(usedAbility);
            priority = usedAbility.priority() + 1;
        }
        return new BusinessProxyFactory<>(ann.code(), ann.priority(), usedAbilities, instance, implExtPoints).getProxy();
    }

    public static UsedAbility resolveUsedAbility(String business, String ability, int priority) throws ProxyParamException {
        if (ability.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] used ability invalid, code should not be empty", business));
        }

        String[] items = ability.split(abilityPriorityDelimiter);
        if (items.length > maxAbilityItemsCnt) {
            throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, format error", business, ability));
        }

        String abilityCode = items[0].trim();
        if (abilityCode.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, code should not be empty", business, ability));
        }

        if (items.length == maxAbilityItemsCnt) {
            try {
                priority = Integer.parseInt(items[1].trim());
            } catch (NumberFormatException e) {
                throw new ProxyParamException(String.format("business [%s] used ability [%s] invalid, priority should be int", business, ability));
            }
        }

        return new UsedAbility(items[0].trim(), priority);
    }
}
