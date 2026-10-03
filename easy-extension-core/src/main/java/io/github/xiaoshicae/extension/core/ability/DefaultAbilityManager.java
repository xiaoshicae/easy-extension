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
    /**
     * Immutable view of the registered abilities: O(1) lookup by code plus the abilities in registration order.
     *
     * @param <T>    matcher param class
     * @param byCode the abilities by code
     * @param all    the abilities in registration order
     */
    private record Snapshot<T>(Map<String, IAbility<T>> byCode, List<IAbility<T>> all) {
    }

    private final Object registrationLock = new Object();

    // Copy-on-write: registration happens at startup and is rare, so it builds a new snapshot under the lock;
    // reads happen on every request and just dereference the current one, with no lock and no copying.
    // Mirrors DefaultBusinessManager so the two managers share one concurrency model.
    private volatile Snapshot<T> snapshot = new Snapshot<>(Collections.emptyMap(), Collections.emptyList());

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

        synchronized (registrationLock) {
            Snapshot<T> current = snapshot;
            if (current.byCode().containsKey(ability.code())) {
                throw new RegisterDuplicateException(String.format("ability [%s] already registered", ability.code()));
            }
            Map<String, IAbility<T>> byCode = new LinkedHashMap<>(current.byCode());
            byCode.put(ability.code(), ability);
            snapshot = new Snapshot<>(
                    Collections.unmodifiableMap(byCode),
                    Collections.unmodifiableList(new ArrayList<>(byCode.values())));
        }
    }

    @Override
    public IAbility<T> getAbility(String abilityCode) throws QueryException {
        IAbility<T> ability = findAbility(abilityCode);
        if (ability == null) {
            throw new QueryNotFoundException(String.format("ability not found by code [%s]", abilityCode));
        }

        return ability;
    }

    @Override
    public IAbility<T> findAbility(String abilityCode) throws QueryException {
        if (abilityCode == null) {
            throw new QueryParamException("abilityCode should not be null");
        }
        return snapshot.byCode().get(abilityCode);
    }

    @Override
    public List<IAbility<T>> listAllAbilities() {
        return snapshot.all();
    }
}
