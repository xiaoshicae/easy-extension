package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;

import java.lang.reflect.Proxy;
import java.util.List;

public class BusinessProxyFactory<T>  {
    private final BusinessTemplate<T> tpl;

    public BusinessProxyFactory(String code, Integer priority, List<UsedAbility> usedAbilities, Matcher<T> businessExtImplInstance, List<Class<?>> implExtPoints) throws ProxyException {
        this(code, priority, usedAbilities, businessExtImplInstance, Utils.classOf(businessExtImplInstance), implExtPoints);
    }

    /**
     * @param code                    code of the business
     * @param priority                priority of the business
     * @param usedAbilities           the abilities the business mounts
     * @param businessExtImplInstance the business implementation
     * @param targetClass             the class that carries the metadata of the business; differs from the class of
     *                                {@code businessExtImplInstance} when that is a proxy created by a container
     * @param implExtPoints           the extension points the business implements
     * @throws ProxyException if {@code targetClass} is null, an extension point is not a public interface, or the
     *                        instance does not implement it
     * @since 3.4
     */
    public BusinessProxyFactory(String code, Integer priority, List<UsedAbility> usedAbilities, Matcher<T> businessExtImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
        this.tpl = new BusinessTemplate<>(code, priority, usedAbilities, businessExtImplInstance, targetClass, implExtPoints);
    }

    @SuppressWarnings("unchecked")
    public IBusiness<T> getProxy() {
        Class<?>[] interfaces = ProxyUtils.buildInterfaces(IBusinessProxy.class, tpl.implementExtensionPoints());
        return (IBusiness<T>) Proxy.newProxyInstance(
                tpl.getInstance().getClass().getClassLoader(),
                interfaces,
                new DelegatingInvocationHandler(tpl, tpl.getInstance(), IBusinessProxy.class)
        );
    }

    public interface IBusinessProxy<T> extends IBusiness<T>, IProxy<Matcher<T>> {
    }

    private static class BusinessTemplate<T> implements IBusinessProxy<T> {
        private final String code;
        private final Integer priority;
        private final List<UsedAbility> usedAbilities;
        private final Matcher<T> extImplInstance;
        private final Class<?> targetClass;
        private final List<Class<?>> implExtPoints;

        public BusinessTemplate(String code, Integer priority, List<UsedAbility> usedAbilities, Matcher<T> extImplInstance, Class<?> targetClass, List<Class<?>> implExtPoints) throws ProxyException {
            Utils.validateInstance(extImplInstance, implExtPoints);
            Utils.validateTargetClass(targetClass);
            this.code = code;
            this.priority = priority;
            this.usedAbilities = usedAbilities;
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

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }
    }

}
