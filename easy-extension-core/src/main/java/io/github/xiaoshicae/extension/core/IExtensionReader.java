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
     * <p>
     * When several default implementations are registered (each for its own extension points) this is the one
     * registered first; {@link #listExtensionPointDefaultImplementations()} lists them all.
     * </p>
     *
     * @return extension point default implementation instance
     */
    IExtensionPointGroupDefaultImplementation<T> getExtensionPointDefaultImplementation();

    /**
     * Get all extension point default implementations, in registration order.
     *
     * @return all default implementations; empty if none is registered
     * @since 4.0
     */
    default List<IExtensionPointGroupDefaultImplementation<T>> listExtensionPointDefaultImplementations() {
        IExtensionPointGroupDefaultImplementation<T> defaultImplementation = getExtensionPointDefaultImplementation();
        return defaultImplementation == null ? List.of() : List.of(defaultImplementation);
    }

    /**
     * Fingerprint of the registry: a short hash over the extension points, businesses, abilities and default
     * implementations (codes, priorities, mounted abilities, implemented extension points). It changes when
     * the registry changes in a way that affects how a request resolves, and is carried by every
     * {@link io.github.xiaoshicae.extension.core.session.ResolvedChain} to tell which registry it belongs to.
     *
     * @return the fingerprint
     * @throws UnsupportedOperationException if this reader does not support it
     * @since 4.0
     */
    default String registryVersion() {
        throw new UnsupportedOperationException("registryVersion() is not supported by this reader");
    }

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
