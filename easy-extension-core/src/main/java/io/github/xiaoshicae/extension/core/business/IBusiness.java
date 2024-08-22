package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.extension.IExtGroupRealization;
import io.github.xiaoshicae.extension.core.priority.ICodePriority;

import java.util.List;

public interface IBusiness<T> extends IExtGroupRealization<T>, ICodePriority {

    /**
     * priority of extension point that implements by business
     *
     * @return priority
     */
    Integer priority();

    /**
     * abilities used by business
     *
     * @return abilities
     */
    List<UsedAbility> usedAbilities();
}
