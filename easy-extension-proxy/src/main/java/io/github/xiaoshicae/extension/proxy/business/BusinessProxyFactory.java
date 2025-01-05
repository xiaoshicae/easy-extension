package io.github.xiaoshicae.extension.proxy.business;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.exception.ProxyParamException;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyDefinition;
import io.github.xiaoshicae.extension.proxy.util.CGLibProxyUtil;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BusinessProxyFactory<T> {
    private static final String abilityPriorityDelimiter = "::";
    private static final Integer maxAbilityItemsCnt = 2;

    @SuppressWarnings("unchecked")
    public IBusiness<T> newBusinessProxy(Class<?> rawBusinessClass) throws ProxyException {
        if (Objects.isNull(rawBusinessClass)) {
            throw new ProxyParamException("class should not be null");
        }
        if (!Matcher.class.isAssignableFrom(rawBusinessClass)) {
            throw new ProxyParamException(String.format("business [%s] must implement [%s]", rawBusinessClass.getSimpleName(), Matcher.class.getName()));
        }
        Business annotation = AnnotationUtils.findAnnotation(rawBusinessClass, Business.class);
        if (Objects.isNull(annotation)) {
            throw new ProxyParamException(String.format("business [%s] must annotated with @Business", rawBusinessClass.getSimpleName()));
        }
        if (annotation.code().isBlank()) {
            throw new ProxyParamException(String.format("code of @Business annotate on [%s] should not be blank", rawBusinessClass.getSimpleName()));
        }
        if (Arrays.stream(annotation.abilities()).anyMatch(String::isBlank)) {
            throw new ProxyParamException(String.format("abilities of @Business annotate on [%s] can not contain blank ability code", rawBusinessClass.getSimpleName()));
        }

        IBusinessProxy<T> businessProxy = new IBusinessProxy<>();
        businessProxy.setCode(annotation.code());
        businessProxy.setPriority(annotation.priority());
        List<Class<?>> extensionPointClasses = Arrays.stream(rawBusinessClass.getInterfaces()).filter((c) -> c.isAnnotationPresent(ExtensionPoint.class)).toList();
        businessProxy.setImplementsExtensions(extensionPointClasses);
        List<UsedAbility> usedAbilities = new ArrayList<>();

        int priority = 1;
        Set<String> abilities = new HashSet<>();
        Set<Integer> priorities = new HashSet<>();
        for (String ability : annotation.abilities()) {
            UsedAbility usedAbility = newUsedAbility(annotation.code(), ability, priority);
            if (abilities.contains(usedAbility.code())) {
                throw new IllegalArgumentException(String.format("abilities of @Business annotate on [%s] contain duplicate code [%s]",rawBusinessClass.getSimpleName(),usedAbility.code()));
            }
            if (priorities.contains(usedAbility.priority())) {
                throw new IllegalArgumentException(String.format("abilities of @Business annotate on [%s] contain duplicate priority [%s]",rawBusinessClass.getSimpleName(),usedAbility.priority()));
            }
            abilities.add(usedAbility.code());
            priorities.add(usedAbility.priority());
            usedAbilities.add(usedAbility);
            priority = usedAbility.priority() + 1;
        }
        businessProxy.setUsedAbilities(usedAbilities);

        CGLibProxyDefinition<IBusiness<?>> definition = new CGLibProxyDefinition<>();
        definition.setInstance(businessProxy);
        definition.setSuperClass(rawBusinessClass);
        definition.setImplInterfaces(IBusiness.class);
        try {
            definition.setMustInvokeSuperMethods(IBusiness.class.getMethod("match", Object.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("method of IBusiness.match(Object.class) not found", e);
        }
        return (IBusiness<T>) CGLibProxyUtil.newCGLibProxy(definition);
    }

    private UsedAbility newUsedAbility(String business, String ability, int priority) throws ProxyParamException {
        if (ability.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] use ability code can not be empty", business));
        }

        String[] items = ability.split(abilityPriorityDelimiter);
        if (items.length > maxAbilityItemsCnt) {
            throw new ProxyParamException(String.format("business [%s] use ability code [%s] priority delimiter[%s] count can not more than %d", business, ability, abilityPriorityDelimiter, maxAbilityItemsCnt - 1));
        }

        String abilityCode = items[0].trim();
        if (abilityCode.isBlank()) {
            throw new ProxyParamException(String.format("business [%s] use ability code [%s], code can not be empty", business, ability));
        }

        if (items.length == maxAbilityItemsCnt) {
            try {
                priority = Integer.parseInt(items[1].trim());
            } catch (NumberFormatException e) {
                throw new ProxyParamException(String.format("business [%s] use ability [%s] with priority must be int", business, ability));
            }
        }

        return new UsedAbility(items[0].trim(), priority);
    }

    public static class IBusinessProxy<T> implements IBusiness<T> {
        private String code;
        private Integer priority;
        private List<UsedAbility> usedAbilities;
        private List<Class<?>> implementsExtensions;

        @Override
        public Boolean match(T param) {
            throw new RuntimeException("invoke match exception, please use superclass match method");
        }

        @Override
        public String code() {
            return code;
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
            return implementsExtensions;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public void setUsedAbilities(List<UsedAbility> usedAbilities) {
            this.usedAbilities = usedAbilities;
        }

        public void setImplementsExtensions(List<Class<?>> implementsExtensions) {
            this.implementsExtensions = implementsExtensions;
        }
    }
}
