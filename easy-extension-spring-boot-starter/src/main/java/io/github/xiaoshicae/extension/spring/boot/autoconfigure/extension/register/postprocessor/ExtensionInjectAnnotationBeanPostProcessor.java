package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import jakarta.annotation.Nonnull;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtensionInjectAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    private final transient Map<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(256);
    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);

    private BeanFactory beanFactory;

    public ExtensionInjectAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(ExtensionInject.class);
    }

    @Override
    public void setBeanFactory(@Nonnull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public PropertyValues postProcessProperties(@Nonnull PropertyValues pvs, Object bean, @Nonnull String beanName) {
        InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed", ex);
        }
        return pvs;
    }

    private InjectionMetadata findResourceMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        InjectionMetadata metadata = injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (injectionMetadataCache) {
                metadata = injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    metadata = buildResourceMetadata(clazz);
                    injectionMetadataCache.put(cacheKey, metadata);
                }
            }
        }
        return metadata;
    }

    private InjectionMetadata buildResourceMetadata(Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, autowiredAnnotationTypes)) {
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
                        throw new BeanCreationException("@ExtensionInject annotation is not supported on static fields: " + field);
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
        protected void inject(@Nonnull Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            String injectBeanName = buildInjectBeanName(field);
            try {
                Object dependency = Objects.requireNonNull(beanFactory).getBean(injectBeanName);
                ReflectionUtils.makeAccessible(field);
                field.set(bean, dependency);
            } catch (IllegalAccessException | BeansException e) {
                throw new BeanCreationException(String.format("%s of class [%s] inject dependency of filed [%s] failed", beanName, bean.getClass(), field.getName()), e);
            }
        }

        private String buildInjectBeanName(Field field) {
            Class<?> fieldType = field.getType();
            if (fieldType == List.class) {
                Class<?> generic = Objects.requireNonNull(getGenericFromField(field));
                return ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(generic.getName());
            }
            return ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(fieldType.getName());
        }

        private Class<?> getGenericFromField(Field field) {
            ResolvableType resolvableType = ResolvableType.forField(field);
//            if (!resolvableType.hasGenerics()) {
//                return null;
//            }
            return resolvableType.getGeneric(0).resolve();
        }
    }

    @Nullable
    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
        MergedAnnotations annotations = MergedAnnotations.from(ao);
        for (Class<? extends Annotation> type : autowiredAnnotationTypes) {
            MergedAnnotation<?> annotation = annotations.get(type);
            if (annotation.isPresent()) {
                return annotation;
            }
        }
        return null;
    }
}
