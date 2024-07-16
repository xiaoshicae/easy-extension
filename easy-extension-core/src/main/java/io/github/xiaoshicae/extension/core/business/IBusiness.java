package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.extension.IExtGroupRealization;
import io.github.xiaoshicae.extension.core.priority.ICodePriority;

import java.util.List;

public interface IBusiness<T> extends IExtGroupRealization<T>, ICodePriority {
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
