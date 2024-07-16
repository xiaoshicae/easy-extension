package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.AbilityException;

import java.util.List;

public interface IAbilityManager<T> {

    /**
     * register ability
     *
     * @param ability @NonNull
     * @throws AbilityException ability invalid
     */
    void registerAbility(IAbility<T> ability) throws AbilityException;

    /**
     * get ability
     *
     * @param abilityCode @NonNull code of ability
     * @throws AbilityException abilityCode is null or ability not found(AbilityNotFoundException)
     */
    IAbility<T> getAbility(String abilityCode) throws AbilityException;

    /**
     * get all abilities
     *
     * @return abilities
     */
    List<IAbility<T>> listAllAbilities();
}
