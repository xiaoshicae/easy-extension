package io.github.xiaoshicae.extension.core.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common InvocationHandler that delegates method calls to either the proxy template
 * or the actual extension implementation instance.
 * <p>
 * If the method is declared on the proxy interface (e.g., IAbilityProxy, IBusinessProxy),
 * it is forwarded to the proxy template. Otherwise, it is forwarded to the real instance.
 * </p>
 */
class DelegatingInvocationHandler implements InvocationHandler {
    private final Object proxyTemplate;
    private final Object realInstance;
    private final Set<Method> proxyMethods;

    /**
     * @param proxyTemplate the proxy template object that handles framework interface methods
     * @param realInstance  the actual extension implementation instance
     * @param proxyInterface the proxy interface class whose methods should be delegated to proxyTemplate
     */
    DelegatingInvocationHandler(Object proxyTemplate, Object realInstance, Class<?> proxyInterface) {
        this.proxyTemplate = proxyTemplate;
        this.realInstance = realInstance;
        this.proxyMethods = Collections.unmodifiableSet(new HashSet<>(List.of(proxyInterface.getMethods())));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return proxyMethods.contains(method) ? method.invoke(proxyTemplate, args) : method.invoke(realInstance, args);
    }
}
