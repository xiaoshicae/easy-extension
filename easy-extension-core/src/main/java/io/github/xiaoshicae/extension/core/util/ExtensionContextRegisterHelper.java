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
    private final List<IExtensionPointGroupDefaultImplementation<T>> additionalDefaultImplementations = new ArrayList<>();
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

    /**
     * Set the default implementation that implements all extension points.
     * To split the defaults up, each answering for some of the extension points, use
     * {@link #addExtensionPointDefaultImplementations(IExtensionPointGroupDefaultImplementation[])} instead.
     */
    public ExtensionContextRegisterHelper<T> setExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) {
        defaultImplementation = instance;
        return this;
    }

    /**
     * Add default implementations that each answer for the extension points they implement; no extension point
     * may be implemented by two of them. Extension points without a default implementation must be
     * {@code @ExtensionPoint(mandatory = true)}.
     *
     * @since 4.0
     */
    @SafeVarargs
    public final ExtensionContextRegisterHelper<T> addExtensionPointDefaultImplementations(IExtensionPointGroupDefaultImplementation<T>... instances) {
        additionalDefaultImplementations.addAll(Arrays.asList(instances));
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

        // 3. register extension point default implementations
        if (defaultImplementation != null) {
            register.registerExtensionPointDefaultImplementation(defaultImplementation);
        }
        for (IExtensionPointGroupDefaultImplementation<T> additional : additionalDefaultImplementations) {
            register.addExtensionPointDefaultImplementation(additional);
        }

        // 4. register abilities
        for (IAbility<T> ability : abilities) {
            register.registerAbility(ability);
        }

        // 5. register businesses
        for (IBusiness<T> business : businesses) {
            register.registerBusiness(business);
        }

        // 6. everything is in: check the registry is complete (every extension point has a default or is mandatory)
        register.validateRegistration();
    }
}
