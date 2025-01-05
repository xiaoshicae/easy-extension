package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.common.Priority;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementation;

import java.util.List;

public interface IBusiness<T> extends IExtensionPointGroupImplementation<T>, Priority {

    /**
     * Business used abilities.
     *
     * @return abilities used by business
     */
    List<UsedAbility> usedAbilities();
}
