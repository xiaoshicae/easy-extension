package io.github.xiaoshicae.extension.proxy.ability;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.exception.ProxyParamException;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyDefinition;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyUtil;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AbilityProxyFactory<T> {

    @SuppressWarnings("unchecked")
    public IAbility<T> newAbilityProxy(Class<?> rawAbilityClass) throws ProxyException {
        if (Objects.isNull(rawAbilityClass)) {
            throw new ProxyParamException("class should not be null");
        }
        if (!Matcher.class.isAssignableFrom(rawAbilityClass)) {
            throw new ProxyParamException(String.format("ability [%s] must implement [%s]", rawAbilityClass.getSimpleName(), Matcher.class.getName()));
        }
        Ability annotation = AnnotationUtils.findAnnotation(rawAbilityClass, Ability.class);
        if (Objects.isNull(annotation)) {
            throw new ProxyParamException(String.format("ability [%s] must annotated with @Ability", rawAbilityClass.getSimpleName()));
        }

        if (annotation.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Ability annotate on [%s] should not be blank", rawAbilityClass.getSimpleName()));
        }

        IAbilityProxy<T> abilityProxy = new IAbilityProxy<>();
        abilityProxy.setCode(annotation.code());
        List<Class<?>> extensionPointClasses = Arrays.stream(rawAbilityClass.getInterfaces()).filter((c) -> c.isAnnotationPresent(ExtensionPoint.class)).toList();
        if (extensionPointClasses.isEmpty()) {
            throw new ProxyParamException(String.format("[%s] should implement at least one interface that annotated with @ExtensionPoint", rawAbilityClass.getSimpleName()));
        }
        abilityProxy.setImplementsExtensions(extensionPointClasses);

        CGLibProxyDefinition<IAbility<?>> definition = new CGLibProxyDefinition<>();
        definition.setInstance(abilityProxy);
        definition.setSuperClass(rawAbilityClass);
        definition.setImplInterfaces(IAbility.class);
        try {
            definition.setMustInvokeSuperMethods(IAbility.class.getMethod("match", Object.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("method of IAbility.match(Object.class) not found", e);
        }
        return (IAbility<T>) CGLibProxyUtil.newCGLibProxy(definition);
    }

    public static class IAbilityProxy<T> implements IAbility<T> {
        private String code;
        private List<Class<?>> implementsExtensions;

        @Override
        public String code() {
            return code;
        }

        @Override
        public Boolean match(T param) {
            throw new RuntimeException("invoke match exception, please use superclass match method");
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return implementsExtensions;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setImplementsExtensions(List<Class<?>> implementsExtensions) {
            this.implementsExtensions = implementsExtensions;
        }
    }
}
