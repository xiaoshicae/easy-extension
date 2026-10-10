package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionContextBuilder;
import io.github.xiaoshicae.extension.core.Resolution;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.definition.DefaultImplementationDefinition;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The {@link ExtensionContext} bean. It exists from the start, so that extension point proxies and other beans can
 * hold on to it, but is built from the beans found in the container only once <em>all singletons are instantiated</em>.
 * That is what lets abilities and businesses themselves use {@code @ExtensionInject}: no bean needs the finished
 * context while it is being created.
 * <p>
 * Consequence: the context cannot be used while beans are still being created (constructors, {@code @PostConstruct},
 * {@code @Bean} factory methods) nor in another {@code SmartInitializingSingleton}, whose callback may run before this
 * one. Use it from a {@code ContextRefreshedEvent} listener or {@code ApplicationRunner} instead. The proxies
 * ({@link #proxy(Class)}, {@code @ExtensionInject}) are bound to this wrapper, so they can be created at any time.
 * </p>
 *
 * @param <T> matcher param type
 */
public class DeferredExtensionContext<T> implements ExtensionContext<T>, SmartInitializingSingleton {
    private final ListableBeanFactory beanFactory;
    private final EasyExtensionConfigurationProperties properties;
    private volatile ExtensionContext<T> delegate;

    public DeferredExtensionContext(ListableBeanFactory beanFactory, EasyExtensionConfigurationProperties properties) {
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.delegate = build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ExtensionContext<T> build() {
        ExtensionContextBuilder<T> builder = ExtensionContext.builder();

        for (ExtensionPointHolder holder : beanFactory.getBeansOfType(ExtensionPointHolder.class).values()) {
            builder.extensionPoint(holder.getExtensionPointClass());
        }
        forEachAnnotated(DefaultImplementation.class, (bean, userClass) -> {
            if (userClass.isAnnotationPresent(DefaultImplementation.class)) {
                builder.defaultImplementation(bean, userClass);
            } else {
                // the annotation sits on the @Bean method (e.g. a lambda for a single-method extension point)
                builder.defaultImplementation(DefaultImplementationDefinition.of(bean));
            }
        });
        forEachAnnotated(Ability.class, builder::ability);
        forEachAnnotated(Business.class, builder::business);
        // providers declared without annotations
        beanFactory.getBeansOfType(DefaultImplementationDefinition.class).values().forEach(builder::defaultImplementation);
        beanFactory.getBeansOfType(AbilityDefinition.class).values().forEach(definition -> builder.ability((AbilityDefinition<T>) definition));
        beanFactory.getBeansOfType(BusinessDefinition.class).values().forEach(definition -> builder.business((BusinessDefinition<T>) definition));

        builder.strict(!properties.isAllowUnknownBusiness());
        builder.matcherParamType(properties.getMatcherParamType());
        return builder.build();
    }

    /**
     * Hands every bean carrying the annotation to the consumer together with its user class: for a Spring proxy
     * (AOP, CGLIB) the class behind it, which is where the annotations and implemented interfaces are read from.
     */
    private void forEachAnnotated(Class<? extends Annotation> annotation, BiConsumer<Object, Class<?>> consumer) {
        for (Map.Entry<String, Object> entry : beanFactory.getBeansWithAnnotation(annotation).entrySet()) {
            Object bean = entry.getValue();
            consumer.accept(bean, AopUtils.getTargetClass(bean));
        }
    }

    private ExtensionContext<T> delegate() {
        ExtensionContext<T> current = delegate;
        if (current == null) {
            throw new IllegalStateException("the ExtensionContext is not ready yet: it is built once all singleton beans are instantiated, "
                    + "so it cannot be used while beans are being created (constructors, @PostConstruct, bean factory methods)");
        }
        return current;
    }

    @Override
    public Resolution resolve(T param) {
        return delegate().resolve(param);
    }

    @Override
    public Binding bind(T param) {
        return delegate().bind(param);
    }

    @Override
    public Binding bind(Resolution resolution) {
        return delegate().bind(resolution);
    }

    @Override
    public Resolution current() {
        return delegate().current();
    }

    @Override
    public boolean isBound() {
        // nothing can be bound before the context is ready
        ExtensionContext<T> current = delegate;
        return current != null && current.isBound();
    }

    @Override
    public void clear() {
        // safety-net cleanup must work, and do nothing, before the context is ready
        ExtensionContext<T> current = delegate;
        if (current != null) {
            current.clear();
        }
    }

    @Override
    public ExtensionCatalog catalog() {
        return delegate().catalog();
    }
}
