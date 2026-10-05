package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects the session-aware extension point proxies into {@link ExtensionInject}-annotated fields
 * (including private and inherited ones). Constructor and method parameters are handled by
 * {@link ExtensionInjectAutowireCandidateResolver}.
 */
public class ExtensionInjectAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    private final Map<Class<?>, List<Field>> injectableFieldsCache = new ConcurrentHashMap<>(256);

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        List<Field> fields = injectableFieldsCache.computeIfAbsent(bean.getClass(), this::findInjectableFields);
        try {
            for (Field field : fields) {
                inject(bean, beanName, field);
            }
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
        }
        return pvs;
    }

    /**
     * Annotated fields of the class and its superclasses, superclass fields first.
     */
    private List<Field> findInjectableFields(Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, ExtensionInject.class)) {
            return List.of();
        }
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            List<Field> declared = new ArrayList<>();
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(ExtensionInject.class)) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new BeanCreationException("@ExtensionInject annotation is not supported on static fields: " + field);
                }
                declared.add(field);
            }
            fields.addAll(0, declared);
        }
        return List.copyOf(fields);
    }

    private void inject(Object bean, String beanName, Field field) {
        String injectBeanName = buildInjectBeanName(field);
        Object dependency;
        try {
            dependency = Objects.requireNonNull(beanFactory).getBean(injectBeanName);
        } catch (BeansException e) {
            throw new BeanCreationException(String.format(
                    "%s of class [%s] failed to resolve @ExtensionInject dependency for field [%s]: no bean [%s] found. " +
                    "Ensure the extension point type is registered via @ExtensionScan or registerExtensionPoint().",
                    beanName, bean.getClass(), field.getName(), injectBeanName), e);
        }
        try {
            ReflectionUtils.makeAccessible(field);
        } catch (RuntimeException e) {
            // e.g. InaccessibleObjectException (JPMS) or SecurityException under a SecurityManager
            throw new BeanCreationException(String.format(
                    "%s of class [%s] cannot make field [%s] accessible for @ExtensionInject. " +
                    "If running under a SecurityManager or JPMS, grant reflective access to the declaring class.",
                    beanName, bean.getClass(), field.getName()), e);
        }
        try {
            field.set(bean, dependency);
        } catch (IllegalAccessException e) {
            throw new BeanCreationException(String.format(
                    "%s of class [%s] inject dependency of field [%s] failed",
                    beanName, bean.getClass(), field.getName()), e);
        }
    }

    private String buildInjectBeanName(Field field) {
        String beanName = ExtensionPointBeanNameGenerator.genInjectBeanName(ResolvableType.forField(field));
        if (beanName == null) {
            throw new BeanCreationException(String.format(
                    "@ExtensionInject on field [%s] of class [%s] requires a generic type parameter, e.g., List<MyExtension>",
                    field.getName(), field.getDeclaringClass().getName()));
        }
        return beanName;
    }
}
