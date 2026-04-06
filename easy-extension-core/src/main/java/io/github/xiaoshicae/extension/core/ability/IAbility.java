package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementation;

import java.util.List;

public interface IAbility<T> extends IExtensionPointGroupImplementation<T> {

    /**
     * Code of ability.
     *
     * @return code of ability
     */
    String code();

    /**
     * Ability match predict.
     *
     * @param param for ability match predict
     * @return if match
     */
    boolean match(T param);

    /**
     * A group of extension point classes of the ability implements.
     *
     * @return extension point classes of the ability implements
     */
    List<Class<?>> implementExtensionPoints();
}
