package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.IExtensionRegister;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Extension context register helper.
 * <p> assist with the registration of extension points, without considering the order of registration. </p>
 * @param <T> matcher param class
 */
public class ExtensionContextRegisterHelper<T> {
    private final Set<Class<?>> allExtensionPointClasses = new LinkedHashSet<>();
    private Class<T> matcherParamClass;
    private IExtensionPointGroupDefaultImplementation<T> defaultImplementation;
    private final List<IAbility<T>> abilities = new ArrayList<>();
    private final List<IBusiness<T>> businesses = new ArrayList<>();

    private final IExtensionRegister<T> register;

    public ExtensionContextRegisterHelper(IExtensionRegister<T> register) {
        this.register = register;
    }

    public ExtensionContextRegisterHelper<T> addExtensionPointClasses(Class<?>... clazz) {
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

    public void doRegister() throws RegisterException {
        // 1. register extension point class
        for (Class<?> clazz : allExtensionPointClasses) {
            register.registerExtensionPoint(clazz);
        }

        // 2. register matcher param class
        register.registerMatcherParamClass(matcherParamClass);

        // 3. register extension point default implementation instance
        register.registerExtensionPointDefaultImplementation(defaultImplementation);

        // 4. register abilities
        for (IAbility<T> ability : abilities) {
            register.registerAbility(ability);
        }

        // 5. register businesses
        for (IBusiness<T> business : businesses) {
            register.registerBusiness(business);
        }
    }
}
