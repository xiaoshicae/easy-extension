package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;

import java.lang.reflect.Proxy;
import java.util.List;

public class AbilityProxyFactory<T>  {
    private final AbilityTemplate<T> tpl;

    public AbilityProxyFactory(String code, Matcher<T> abilityExtImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this(code, abilityExtImplInstance, Utils.classOf(abilityExtImplInstance), implExtPoints);
    }

    /**
     * @param code                   code of the ability
     * @param abilityExtImplInstance the ability implementation
     * @param targetClass            the class that carries the metadata of the ability; differs from the class of
     *                               {@code abilityExtImplInstance} when that is a proxy created by a container
     * @param implExtPoints          the extension points the ability implements
     * @throws ProxyException if {@code targetClass} is null, an extension point is not a public interface, or the
     *                        instance does not implement it
     * @since 3.4
     */
    public AbilityProxyFactory(String code, Matcher<T> abilityExtImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new AbilityTemplate<>(code, abilityExtImplInstance, targetClass, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IAbility<T> getProxy() {
        Class<?>[] interfaces = ProxyUtils.buildInterfaces(IAbilityProxy.class, tpl.implementExtensionPoints());
        return (IAbility<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new DelegatingInvocationHandler(tpl, tpl.getInstance(), IAbilityProxy.class)
        );
    }

    public interface IAbilityProxy<T> extends IAbility<T>, IProxy<Matcher<T>> {
    }

    private static class AbilityTemplate<T> implements IAbilityProxy<T> {
        private final String code;
        private final Matcher<T> extImplInstance;
        private final Class<?> targetClass;
        private final List<Class<?>> implExtPoints;

        public AbilityTemplate(String code, Matcher<T> extImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
            Utils.validateNoClash("an ability", IAbilityProxy.class, implExtPoints);
            Utils.validateTargetClass(targetClass);
            this.code = code;
            this.extImplInstance = extImplInstance;
            this.targetClass = targetClass;
            this.implExtPoints = implExtPoints;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public boolean match(T param) {
            return extImplInstance.match(param);
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return implExtPoints;
        }

        @Override
        public Matcher<T> getInstance() {
            return extImplInstance;
        }

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }
    }
}
