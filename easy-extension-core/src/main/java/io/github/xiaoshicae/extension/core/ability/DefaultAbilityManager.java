package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.exception.AbilityException;
import io.github.xiaoshicae.extension.core.exception.AbilityNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultAbilityManager<T> implements IAbilityManager<T> {
    private final Map<String, IAbility<T>> abilities = new LinkedHashMap<>();

    @Override
    public void registerAbility(IAbility<T> ability) throws AbilityException {
        if (ability == null) {
            throw new AbilityException("ability can not be null");
        }
        if (abilities.containsKey(ability.code())) {
            throw new AbilityException("ability " + ability.code() + " already registered");
        }

        boolean existExtensionAnnotation = false;
        for (Class<?> clazz : ability.getClass().getInterfaces()) {
            if (clazz.isAnnotationPresent(ExtensionPoint.class)) {
                existExtensionAnnotation = true;
                break;
            }
        }
        if (!existExtensionAnnotation) {
            throw new AbilityException("ability " + ability.code() + " should implement at least one interface that annotated with @ExtensionPoint");
        }

        abilities.put(ability.code(), ability);
    }

    @Override
    public IAbility<T> getAbility(String abilityCode) throws AbilityException {
        if (abilityCode == null) {
            throw new AbilityException("abilityCode can not be null");
        }
        IAbility<T> ability = abilities.get(abilityCode);
        if (ability == null) {
            throw new AbilityNotFoundException("ability " + abilityCode + " not found");
        }
        return ability;
    }

    @Override
    public List<IAbility<T>> listAllAbilities() {
        return new ArrayList<>(abilities.values());
    }
}
