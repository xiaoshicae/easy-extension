package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensionInjectAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

    private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

    private final ExtensionBeanNameGenerator extensionBeanNameGenerator = ExtensionBeanNameGenerator.INSTANCE;

    @Nullable
    private BeanFactory beanFactory;

    public ExtensionInjectAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(ExtensionInject.class);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
        }
        return pvs;
    }

    private InjectionMetadata findResourceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    metadata = buildResourceMetadata(clazz);
                    this.injectionMetadataCache.put(cacheKey, metadata);
                }
            }
        }
        return metadata;
    }

    private InjectionMetadata buildResourceMetadata(Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
            return InjectionMetadata.EMPTY;
        }

        final List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        Class<?> targetClass = clazz;

        do {
            final List<InjectionMetadata.InjectedElement> fieldElements = new ArrayList<>();
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                MergedAnnotation<?> ann = findAutowiredAnnotation(field);
                if (ann != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        throw new BeanCreationException("ExtensionInject annotation is not supported on static fields: " + field);
                    }
                    fieldElements.add(new AutowiredFieldElement(field));
                }
            });
            elements.addAll(0, fieldElements);
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);

        return InjectionMetadata.forElements(elements, clazz);
    }

    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        public AutowiredFieldElement(Field field) {
            super(field, null);
        }

        @Override
        protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            if (!checkFieldTypeIsAnnWithExtensionPoint(field)) {
                throw new BeanCreationException(bean.getClass() + " failed to inject dependency of filed " + field.getName() + ", filed type must be interface or list<interface> (interface that with @ExtensionPoint annotation), but got " + field.getType().getName());
            }
            String injectBeanName = buildInjectBeanName(field);
            Object dependency = beanFactory.getBean(injectBeanName);
            try {
                ReflectionUtils.makeAccessible(field);
                field.set(bean, dependency);
            } catch (IllegalAccessException e) {
                throw new BeanCreationException(bean.getClass() + " failed to inject dependency of filed " + field.getName() + ", exception:", e);
            }
        }

        private boolean checkFieldTypeIsAnnWithExtensionPoint(Field field) {
            Class<?> type = field.getType();
            if (type == List.class) {
                type = getGenericFromField(field);
            }
            if (type == null) {
                return false;
            }
            return type.isAnnotationPresent(ExtensionPoint.class);
        }

        private String buildInjectBeanName(Field field) {
            Class<?> fieldType = field.getType();
            if (fieldType == List.class) {
                Class<?> generic = getGenericFromField(field);
                return extensionBeanNameGenerator.genExtensionListBeanName(generic.getName());
            }
            return extensionBeanNameGenerator.genExtensionBeanName(fieldType.getName());
        }

        private Class<?> getGenericFromField(Field field) {
            ResolvableType resolvableType = ResolvableType.forField(field);
            if (!resolvableType.hasGenerics()) {
                return null;
            }
            return resolvableType.getGeneric(0).resolve();
        }
    }

    @Nullable
    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
        MergedAnnotations annotations = MergedAnnotations.from(ao);
        for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
            MergedAnnotation<?> annotation = annotations.get(type);
            if (annotation.isPresent()) {
                return annotation;
            }
        }
        return null;
    }
}
