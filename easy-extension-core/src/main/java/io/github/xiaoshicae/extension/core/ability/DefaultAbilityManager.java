package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultAbilityManager<T> implements IAbilityManager<T> {
    // Synchronized LinkedHashMap for O(1) lookup and ordered iteration.
    // Mirrors DefaultBusinessManager so the two managers share one concurrency model.
    private final Map<String, IAbility<T>> abilities = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void registerAbility(IAbility<T> ability) throws RegisterException {
        if (ability == null) {
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

        synchronized (abilities) {
            if (abilities.containsKey(ability.code())) {
                throw new RegisterDuplicateException(String.format("ability [%s] already registered", ability.code()));
            }
            abilities.put(ability.code(), ability);
        }
    }

    @Override
    public IAbility<T> getAbility(String abilityCode) throws QueryException {
        if (abilityCode == null) {
            throw new QueryParamException("abilityCode should not be null");
        }

        IAbility<T> ability = abilities.get(abilityCode);
        if (ability == null) {
            throw new QueryNotFoundException(String.format("ability not found by code [%s]", abilityCode));
        }

        return ability;
    }

    @Override
    public List<IAbility<T>> listAllAbilities() {
        synchronized (abilities) {
            return Collections.unmodifiableList(new ArrayList<>(abilities.values()));
        }
    }
}
