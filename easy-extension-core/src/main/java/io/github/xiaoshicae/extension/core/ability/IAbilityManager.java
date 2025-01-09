package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.List;

public interface IAbilityManager<T> {

    /**
     * Register ability by code.
     *
     * @param ability ability instance
     * @throws RegisterParamException     if {@code ability} is null, implements extension points is empty or
     *                                    implemented extension point class not an interface type,
     *                                    or {@code ability} is not instance of extension point class
     * @throws RegisterDuplicateException if ability by code duplicate register
     */
    void registerAbility(IAbility<T> ability) throws RegisterException;

    /**
     * Get ability by code.
     *
     * @param abilityCode code of ability
     * @throws QueryParamException    if {@code abilityCode} is null
     * @throws QueryNotFoundException if ability not found
     */
    IAbility<T> getAbility(String abilityCode) throws QueryException;

    /**
     * Get all abilities.
     *
     * @return all abilities
     */
    List<IAbility<T>> listAllAbilities();
}
