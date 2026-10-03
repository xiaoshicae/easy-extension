package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;

public interface IExtensionRegister<T> {

    /**
     * Register extension point class,
     * class must be an interface type.
     *
     * @param clazz class of extension point
     * @throws RegisterParamException     if {@code clazz} is null or not an interface type
     * @throws RegisterDuplicateException if {@code clazz} duplicate register
     */
    void registerExtensionPoint(Class<?> clazz) throws RegisterException;

    /**
     * Register matcher param class.
     *
     * @param matcherParamClass class of matcher param
     * @throws RegisterParamException     if matcher param class is null
     * @throws RegisterDuplicateException if matcher param class duplicate register
     */
    void registerMatcherParamClass(Class<T> matcherParamClass) throws RegisterException;


    /**
     * Register extension point default implementation instance,
     * the instance must implement all extension point.
     *
     * @param instance extension point default implementation instance
     * @throws RegisterParamException     if {@code instance} is null or {@code instance} not implement all the extension point class
     * @throws RegisterDuplicateException if {@code instance} duplicate register
     */
    void registerExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException;


    /**
     * Register a default implementation for the extension points it implements, next to the one registered with
     * {@link #registerExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation)}, if any.
     * <p>
     * That method expects a single default implementation that implements <em>all</em> extension points. This one is
     * for splitting the defaults up, for example one per domain: call it once per default implementation. Each
     * extension point may have only one default implementation, and extension points without any must be
     * {@code @ExtensionPoint(mandatory = true)}, which {@link #validateRegistration()} checks.
     * </p>
     *
     * @param instance default implementation of (some of) the extension points
     * @throws RegisterParamException     if {@code instance} is null or implements no extension point
     * @throws RegisterDuplicateException if an extension point {@code instance} implements already has a default
     *                                    implementation
     * @throws UnsupportedOperationException if this register does not support several default implementations
     * @since 4.0
     */
    default void addExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException {
        throw new UnsupportedOperationException("addExtensionPointDefaultImplementation() is not supported by this register");
    }

    /**
     * Register an interceptor that wraps every call to an extension implementation; see {@link ExtensionInterceptor}.
     * Interceptors are applied in registration order, the first registered being the outermost.
     *
     * @param interceptor the interceptor
     * @throws RegisterParamException        if {@code interceptor} is null
     * @throws UnsupportedOperationException if this register does not support interceptors
     * @since 4.0
     */
    default void registerInterceptor(ExtensionInterceptor interceptor) throws RegisterException {
        throw new UnsupportedOperationException("registerInterceptor() is not supported by this register");
    }

    /**
     * Check that the registry is complete, once everything is registered: every extension point that is not
     * {@code @ExtensionPoint(mandatory = true)} must have a default implementation. Registration itself stays
     * order-independent, which is why this is a separate step; {@code ExtensionContextRegisterHelper#doRegister()}
     * and the Spring starter call it for you.
     *
     * @throws RegisterParamException if an extension point has neither a default implementation nor is mandatory
     * @since 4.0
     */
    default void validateRegistration() throws RegisterException {
    }

    /**
     * Register ability.
     *
     * @param ability ability instance
     * @throws RegisterParamException     if {@code ability} is null
     * @throws RegisterDuplicateException if {@code ability} by code duplicate register
     */
    void registerAbility(IAbility<T> ability) throws RegisterException;

    /**
     * Register business.
     *
     * @param business business instance
     * @throws RegisterParamException     if {@code business} is null
     * @throws RegisterDuplicateException if {@code business} by code duplicate register
     */
    void registerBusiness(IBusiness<T> business) throws RegisterException;
}
