package io.github.xiaoshicae.extension.proxy;

import io.github.xiaoshicae.extension.core.ExtensionContextRegisterHelper;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.proxy.ability.AbilityProxyFactory;
import io.github.xiaoshicae.extension.proxy.business.BusinessProxyFactory;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.defaultimpl.ExtensionPointDefaultImplProxyFactory;

public class ExtensionContextProxyRegisterHelper<T> {
    ExtensionContextRegisterHelper<T> extensionContextRegisterHelper = new ExtensionContextRegisterHelper<>();
    ExtensionPointDefaultImplProxyFactory<T> extensionPointDefaultImplProxyFactory = new ExtensionPointDefaultImplProxyFactory<>();
    AbilityProxyFactory<T> abilityProxyFactory = new AbilityProxyFactory<>();
    BusinessProxyFactory<T> businessProxyFactory = new BusinessProxyFactory<>();

    ExtensionContextProxyRegisterHelper() {
    }

    public ExtensionContextProxyRegisterHelper<T> addExtensionPointClasses(Class<?> ...clazz) {
        extensionContextRegisterHelper.addExtensionPointClasses(clazz);
        return this;
    }

    public ExtensionContextProxyRegisterHelper<T> setMatcherParamClass(Class<T> clazz) {
        extensionContextRegisterHelper.setMatcherParamClass(clazz);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ExtensionContextProxyRegisterHelper<T> setExtensionPointDefaultImplementation(Object instance) throws ProxyException {
        if (instance instanceof IExtensionPointGroupDefaultImplementation<?> im) {
            IExtensionPointGroupDefaultImplementation<T> implementation = (IExtensionPointGroupDefaultImplementation<T>)im;
            extensionContextRegisterHelper.setExtensionPointDefaultImplementation(implementation);
            return this;
        }
        IExtensionPointGroupDefaultImplementation<T> instanceProxy = extensionPointDefaultImplProxyFactory.newExtensionPointDefaultImplProxy(instance.getClass());
        extensionContextRegisterHelper.setExtensionPointDefaultImplementation(instanceProxy);
        return this;
    }

    @SuppressWarnings("unchecked")
    public final ExtensionContextProxyRegisterHelper<T> addAbilities(Object... abilities) throws ProxyException {
        for (Object ability : abilities) {
            IAbility<T> a;
            if (ability instanceof IAbility<?> ab) {
                a = (IAbility<T>) ab;
            } else {
                a = abilityProxyFactory.newAbilityProxy(ability.getClass());
            }
            extensionContextRegisterHelper.addAbilities(a);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public final ExtensionContextProxyRegisterHelper<T> addBusinesses(Object... businesses) throws ProxyException {
        for (Object business : businesses) {
            IBusiness<T> b;
            if (business instanceof IBusiness<?> biz) {
                b = (IBusiness<T>) biz;
            } else {
                b = businessProxyFactory.newBusinessProxy(business.getClass());
            }
            extensionContextRegisterHelper.addBusinesses(b);
        }
        return this;
    }

    public void doRegister(IExtensionContext<T> extensionContext) throws RegisterException {
        extensionContextRegisterHelper.doRegister(extensionContext);
    }
}
