package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.interfaces.Priority;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementation;

import java.util.List;

public interface IBusiness<T> extends IExtensionPointGroupImplementation<T>, Priority {

    /**
     * Code of business.
     *
     * @return code of business
     */
    String code();

    /**
     * Business match predict.
     *
     * @param param for business match predict
     * @return if match
     */
    Boolean match(T param);

    /**
     * Priority of business,
     * compare with priority of abilities used by business.
     *
     * @return priority of business
     */
    Integer priority();

    /**
     * Abilities used by business.
     *
     * @return abilities used by business
     */
    List<UsedAbility> usedAbilities();

    /**
     * A group of extension point classes of the business implements
     *
     * @return extension point classes of the business implements
     */
    List<Class<?>> implementExtensionPoints();
}
