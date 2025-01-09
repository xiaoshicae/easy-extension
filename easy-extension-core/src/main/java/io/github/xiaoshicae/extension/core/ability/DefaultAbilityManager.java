package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultAbilityManager<T> implements IAbilityManager<T> {
    private final Map<String, IAbility<T>> abilities = new ConcurrentHashMap<>();
    private final List<String> abilityCodes = new CopyOnWriteArrayList<>();

    @Override
    public void registerAbility(IAbility<T> ability) throws RegisterException {
        if (Objects.isNull(ability)) {
            throw new RegisterParamException("ability should not be null");
        }

        if (ability.implementExtensionPoints().isEmpty()) {
            throw new RegisterParamException(String.format("ability [%s] should implement at least one extension point", ability.code()));
        }

        for (Class<?> clazz : ability.implementExtensionPoints()) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("ability [%s] implement extension point class [%s] invalid, class should be an interface type", ability.code(), clazz.getName()));
            }
            if (!clazz.isInstance(ability)) {
                throw new RegisterParamException(String.format("ability [%s] not implement extension point class [%s]", ability.code(), clazz.getName()));
            }
        }

        if (abilities.containsKey(ability.code())) {
            throw new RegisterDuplicateException(String.format("ability [%s] already registered", ability.code()));
        }

        abilities.put(ability.code(), ability);
        abilityCodes.add(ability.code());
    }

    @Override
    public IAbility<T> getAbility(String abilityCode) throws QueryException {
        if (Objects.isNull(abilityCode)) {
            throw new QueryParamException("abilityCode should not be null");
        }

        IAbility<T> ability = abilities.get(abilityCode);
        if (Objects.isNull(ability)) {
            throw new QueryNotFoundException(String.format("ability not found by code [%s]", abilityCode));
        }

        return ability;
    }

    @Override
    public List<IAbility<T>> listAllAbilities() {
        List<IAbility<T>> allAbilities = new ArrayList<>(abilityCodes.size());
        abilityCodes.forEach(code -> allAbilities.add(abilities.get(code)));
        return allAbilities;
    }
}
