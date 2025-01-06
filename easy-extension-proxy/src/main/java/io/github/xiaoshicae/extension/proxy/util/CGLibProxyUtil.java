package io.github.xiaoshicae.extension.proxy.util;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CGLibProxyUtil {
    private final static String toStringMethodName = "toString";

    @SuppressWarnings("unchecked")
    public static <T> T newCGLibProxy(CGLibProxyDefinition<T> definition) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(definition.getSuperClass());
        enhancer.setInterfaces(definition.getImplInterfaces());

        T instance = definition.getInstance();
        List<Method> mustInvokeSuperMethods = resolveMustInvokeSuperMethods(definition);
        List<Method> implInterfaceMethods = resolveImplInterfacesMethods(definition);

        enhancer.setCallback((MethodInterceptor) (object, method, args, methodProxy) -> {
            if (mustInvokeSuperMethods.stream().anyMatch(m -> methodEqual(m, method))) {
                return methodProxy.invokeSuper(object, args);
            }
            Optional<Method> matchedMethod = implInterfaceMethods.stream().filter(m -> methodEqual(m, method)).findFirst();
            if (matchedMethod.isPresent()) {
                return matchedMethod.get().invoke(instance, args);
            }
            if (method.getName().equals(toStringMethodName)) {
                return "CGLIB proxy for Instance<" + definition.getSuperClass().getSimpleName() + ">";
            }
            return methodProxy.invokeSuper(object, args);
        });

        return (T) enhancer.create();
    }


    private static List<Method> resolveMustInvokeSuperMethods(CGLibProxyDefinition<?> definition) {
        Method[] superMethods = definition.getMustInvokeSuperMethods();
        if (Objects.isNull(superMethods)) {
            return new ArrayList<>();
        }
        return Arrays.stream(superMethods).toList();
    }

    private static List<Method> resolveImplInterfacesMethods(CGLibProxyDefinition<?> definition) {
        Class<?>[] implInterfaces = definition.getImplInterfaces();
        if (Objects.isNull(implInterfaces)) {
            return new ArrayList<>();
        }
        return Arrays.stream(implInterfaces).flatMap(clazz -> Arrays.stream(clazz.getMethods())).toList();
    }

    public static boolean methodEqual(Method m1, Method m2) {
        if (!m1.getName().equals(m2.getName())) {
            return false;
        }

        Class<?>[] paramTypes1 = m1.getParameterTypes();
        Class<?>[] paramTypes2 = m2.getParameterTypes();
        if (paramTypes1.length != paramTypes2.length) {
            return false;
        }
        for (int i = 0; i < paramTypes1.length; i++) {
            if (paramTypes1[i] != paramTypes2[i]) {
                return false;
            }
        }
        return m1.getReturnType() == m2.getReturnType();
    }
}
