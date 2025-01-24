package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BusinessProxyFactory<T>  {
    private final BusinessTemplate<T> tpl;

    public BusinessProxyFactory(String code, Integer priority, List<UsedAbility> usedAbilities, Matcher<T> businessExtImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new BusinessTemplate<>(code, priority, usedAbilities, businessExtImplInstance, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IBusiness<T> getProxy() {
        Class<?>[] interfaces = new Class[tpl.implementExtensionPoints().size() + 1];
        interfaces[0] = IBusinessProxy.class;
        for (int i = 0; i < tpl.implementExtensionPoints().size(); i++) {
            interfaces[i + 1] = tpl.implementExtensionPoints().get(i);
        }
        return (IBusiness<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new BusinessInvocationHandler<>(tpl, tpl.getInstance())
        );
    }

    public interface IBusinessProxy<T> extends IBusiness<T>, IProxy<Matcher<T>> {
    }

    private static class BusinessTemplate<T> implements IBusinessProxy<T> {
        private final String code;
        private final Integer priority;
        private final List<UsedAbility> usedAbilities;
        private final Matcher<T> extImplInstance;
        private final List<Class<?>> implExtPoints;

        public BusinessTemplate(String code, Integer priority, List<UsedAbility> usedAbilities, Matcher<T> extImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
            this.code = code;
            this.priority = priority;
            this.usedAbilities = usedAbilities;
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
        public Integer priority() {
            return priority;
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            return usedAbilities;
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

    private static class BusinessInvocationHandler<T> implements InvocationHandler {
        private final IBusiness<T> businessProxy;
        private final Object businessExtImplInstance;
        private static final Set<Method> businessProxyMethodCache = new HashSet<>(List.of(IBusinessProxy.class.getMethods()));

        public BusinessInvocationHandler(IBusiness<T> businessProxy, Object businessExtImplInstance) {
            this.businessProxy = businessProxy;
            this.businessExtImplInstance = businessExtImplInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return businessProxyMethodCache.contains(method) ? method.invoke(businessProxy, args) : method.invoke(businessExtImplInstance, args);
        }
    }
}
