package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.AbilityException;

import java.util.List;

public interface IAbilityManager<T> {

    /**
     * register ability with the manager
     *
     * @param ability ability that need to be registered with the manager
     * @throws AbilityException ability invalid (ability null or ability has been registered or ...)
     */
    void registerAbility(IAbility<T> ability) throws AbilityException;

    /**
     * get ability from the manager
     *
     * @param abilityCode code of ability
     * @throws AbilityException abilityCode is null or ability not found
     */
    IAbility<T> getAbility(String abilityCode) throws AbilityException;

    /**
     * get all abilities from the manager
     *
     * @return abilities
     */
    List<IAbility<T>> listAllAbilities();
}
