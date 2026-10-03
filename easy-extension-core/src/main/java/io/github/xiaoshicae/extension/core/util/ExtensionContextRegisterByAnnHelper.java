package io.github.xiaoshicae.extension.core.util;


import io.github.xiaoshicae.extension.core.IExtensionRegister;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;


/**
 * Extension context register helper.
 * <p> Assist with the registration of extension points annotated with @Business, @Ability or @ExtensionPointGroupDefaultImplementation,
 * without considering the order of registration. </p>
 *
 * @param <T> matcher param class
 */
public class ExtensionContextRegisterByAnnHelper<T> {
    private final ExtensionContextRegisterHelper<T> extensionContextRegisterHelper;

    public ExtensionContextRegisterByAnnHelper(IExtensionRegister<T> register) {
        this.extensionContextRegisterHelper = new ExtensionContextRegisterHelper<>(register);
    }

    public ExtensionContextRegisterByAnnHelper<T> addExtensionPointClasses(Class<?>... clazz) {
        extensionContextRegisterHelper.addExtensionPointClasses(clazz);
        return this;
    }

    public ExtensionContextRegisterByAnnHelper<T> setMatcherParamClass(Class<T> clazz) {
        extensionContextRegisterHelper.setMatcherParamClass(clazz);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ExtensionContextRegisterByAnnHelper<T> setExtensionPointDefaultImplementation(Object instance) throws ProxyException {
        if (instance instanceof IExtensionPointGroupDefaultImplementation<?> defaultImpl) {
            extensionContextRegisterHelper.setExtensionPointDefaultImplementation((IExtensionPointGroupDefaultImplementation<T>) defaultImpl);
            return this;
        }
        extensionContextRegisterHelper.setExtensionPointDefaultImplementation(
                AnnProxyConvertUtils.<T>convertAnnExtensionPointGroupDefaultImplementation(instance));
        return this;
    }

    /**
     * Add default implementations that each answer for the extension points they implement; see
     * {@link ExtensionContextRegisterHelper#addExtensionPointDefaultImplementations(IExtensionPointGroupDefaultImplementation[])}.
     *
     * @since 4.0
     */
    @SuppressWarnings("unchecked")
    public ExtensionContextRegisterByAnnHelper<T> addExtensionPointDefaultImplementations(Object... instances) throws ProxyException {
        for (Object instance : instances) {
            IExtensionPointGroupDefaultImplementation<T> defaultImpl = instance instanceof IExtensionPointGroupDefaultImplementation<?> d
                    ? (IExtensionPointGroupDefaultImplementation<T>) d
                    : AnnProxyConvertUtils.<T>convertAnnExtensionPointGroupDefaultImplementation(instance);
            extensionContextRegisterHelper.addExtensionPointDefaultImplementations(defaultImpl);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public final ExtensionContextRegisterByAnnHelper<T> addAbilities(Matcher<T>... abilities) throws ProxyException {
        for (Matcher<T> ability : abilities) {
            IAbility<T> ab = ability instanceof IAbility<?> ? (IAbility<T>) ability : AnnProxyConvertUtils.convertAnnAbilityToProxy(ability);
            extensionContextRegisterHelper.addAbilities(ab);
        }
        return this;
    }


    @SuppressWarnings("unchecked")
    public final ExtensionContextRegisterByAnnHelper<T> addBusinesses(Matcher<T>... businesses) throws ProxyException {
        for (Matcher<T> business : businesses) {
            IBusiness<T> biz = business instanceof IBusiness<T> ? (IBusiness<T>) business : AnnProxyConvertUtils.convertAnnBusinessToProxy(business);
            extensionContextRegisterHelper.addBusinesses(biz);
        }
        return this;
    }

    public void doRegister() throws RegisterException {
        extensionContextRegisterHelper.doRegister();
    }
}
