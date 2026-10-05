package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAbilityManager<T> implements IAbilityManager<T> {
    // Mirrors DefaultBusinessManager so the two managers share one concurrency model:
    // concurrent map for lookups, immutable snapshot (registration order) for iteration.
    private final Map<String, IAbility<T>> abilities = new ConcurrentHashMap<>();
    private volatile List<IAbility<T>> snapshot = List.of();

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

        if (ability.code() == null) {
            throw new RegisterParamException("instance code should not be null");
        }

        synchronized (this) {
            if (abilities.containsKey(ability.code())) {
                throw new RegisterDuplicateException(String.format("ability [%s] already registered", ability.code()));
            }
            abilities.put(ability.code(), ability);
            List<IAbility<T>> next = new ArrayList<>(snapshot);
            next.add(ability);
            snapshot = Collections.unmodifiableList(next);
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
        return snapshot;
    }
}
