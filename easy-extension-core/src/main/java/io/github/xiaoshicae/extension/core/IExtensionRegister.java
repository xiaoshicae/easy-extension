package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

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
