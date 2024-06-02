package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.extension.IExtGroupRealization;

import java.util.List;

public interface IBusiness<T> extends IExtGroupRealization<T> {
    /**
     * effective priority of business
     *
     * @return priority
     */
    Integer priority();

    /**
     * all abilities used by business
     *
     * @return abilities
     */
    List<UsedAbility> usedAbilities();
}
