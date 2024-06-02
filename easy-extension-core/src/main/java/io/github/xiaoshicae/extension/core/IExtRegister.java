package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;

public interface IExtRegister<T> {
    /**
     * register ability to factory
     *
     * @param ability @NonNull
     * @throws ExtensionException ability invalid
     */
    void registerAbility(IAbility<T> ability) throws ExtensionException;

    /**
     * register business to factory
     *
     * @param business @NonNull
     * @throws ExtensionException business invalid
     */
    void registerBusiness(IBusiness<T> business) throws ExtensionException;
}
