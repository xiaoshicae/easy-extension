package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.util.List;

public interface IExtensionReader<T> {

    /**
     * Get all extension point class.
     * Please note that the list may be unordered.
     *
     * @return all extension point class
     */
    List<Class<?>> listAllExtensionPoint();

    /**
     * Get matcher param class.
     *
     * @return matcher param class
     */
    Class<T> getMatcherParamClass();

    /**
     * Get extension point default implementation instance.
     *
     * @return extension point default implementation instance
     */
    IExtensionPointGroupDefaultImplementation<T> getExtensionPointDefaultImplementation();

    /**
     * Get all ability instance.
     *
     * @return all ability instance
     */
    List<IAbility<T>> listAllAbility();

    /**
     * Get all business.
     *
     * @return all business instance
     */
    List<IBusiness<T>> listAllBusiness();
}
