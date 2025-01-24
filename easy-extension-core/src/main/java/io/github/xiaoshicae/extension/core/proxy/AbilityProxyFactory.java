package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbilityProxyFactory<T>  {
    private final AbilityTemplate<T> tpl;

    public AbilityProxyFactory(String code, Matcher<T> abilityExtImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new AbilityTemplate<>(code, abilityExtImplInstance, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IAbility<T> getProxy() {
        Class<?>[] interfaces = new Class[tpl.implementExtensionPoints().size() + 1];
        interfaces[0] = IAbilityProxy.class;
        for (int i = 0; i < tpl.implementExtensionPoints().size(); i++) {
            interfaces[i + 1] = tpl.implementExtensionPoints().get(i);
        }
        return (IAbility<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new AbilityInvocationHandler<>(tpl, tpl.getInstance())
        );
    }

    public interface IAbilityProxy<T> extends IAbility<T>, IProxy<Matcher<T>> {
    }

    private static class AbilityTemplate<T> implements IAbilityProxy<T> {
        private final String code;
        private final Matcher<T> extImplInstance;
        private final List<Class<?>> implExtPoints;

        public AbilityTemplate(String code, Matcher<T> extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
            this.code = code;
            this.extImplInstance = extImplInstance;
            this.implExtPoints = implExtPoints;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public Boolean match(T param) {
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
    }

    private static class AbilityInvocationHandler<T> implements InvocationHandler {
        private final IAbility<T> abilityProxy;
        private final Object abilityExtImplInstance;
        private static final Set<Method> abilityProxyMethodCache = new HashSet<>(List.of(IAbilityProxy.class.getMethods()));

        public AbilityInvocationHandler(IAbility<T> abilityProxy, Object abilityExtImplInstance) {
            this.abilityProxy = abilityProxy;
            this.abilityExtImplInstance = abilityExtImplInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return abilityProxyMethodCache.contains(method) ? method.invoke(abilityProxy, args) : method.invoke(abilityExtImplInstance, args);
        }
    }
}
