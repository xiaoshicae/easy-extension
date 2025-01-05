package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExtensionContextRegisterHelper<T> {
    private final Set<Class<?>> allExtensionPointClasses = new LinkedHashSet<>();
    private Class<T> matcherParamClass;
    private IExtensionPointGroupDefaultImplementation<T> defaultImplementation;
    private final List<IAbility<T>> abilities = new ArrayList<>();
    private final List<IBusiness<T>> businesses = new ArrayList<>();

    public ExtensionContextRegisterHelper() {}

    public ExtensionContextRegisterHelper<T> addExtensionPointClasses(Class<?> ...clazz) {
        allExtensionPointClasses.addAll(Arrays.asList(clazz));
        return this;
    }

    public ExtensionContextRegisterHelper<T> setMatcherParamClass(Class<T> clazz) {
        matcherParamClass = clazz;
        return this;
    }

    public ExtensionContextRegisterHelper<T> setExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) {
        defaultImplementation = instance;
        return this;
    }

    @SafeVarargs
    public final ExtensionContextRegisterHelper<T> addAbilities(IAbility<T>... ability) {
        abilities.addAll(Arrays.asList(ability));
        return this;
    }

    @SafeVarargs
    public final ExtensionContextRegisterHelper<T> addBusinesses(IBusiness<T>... business) {
        businesses.addAll(Arrays.asList(business));
        return this;
    }

    public void doRegister(IExtensionContext<T> extensionContext) throws RegisterException {
        for (Class<?> clazz : allExtensionPointClasses) {
            extensionContext.registerExtensionPoint(clazz);
        }
        extensionContext.registerMatcherParamClass(matcherParamClass);
        extensionContext.registerExtensionPointDefaultImplementation(defaultImplementation);
        for (IAbility<T> ability : abilities) {
            extensionContext.registerAbility(ability);
        }
        for (IBusiness<T> business : businesses) {
            extensionContext.registerBusiness(business);
        }
    }
}
