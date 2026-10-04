package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterByAnnHelper;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ClassHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.InstanceHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


@Configuration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
public class EasyExtensionAutoConfiguration<T> {
    private static final Logger logger = LoggerFactory.getLogger(EasyExtensionAutoConfiguration.class);

    private List<ExtensionPointHolder> extensionPointHolders;
    private List<IExtensionPointGroupDefaultImplementation<T>> extensionPointGroupImplementations;
    private IScopedSessionManager sessionManager;
    private BusinessMatchSelector<T> businessMatchSelector;
    private List<ExtensionInterceptor> interceptors;
    private ObjectProvider<ExtensionInterceptor> interceptorBeans;
    private volatile DefaultExtensionContext<T> createdContext;
    private volatile boolean singletonsInstantiated;
    private final AtomicBoolean interceptorBeansRegistered = new AtomicBoolean();
    private List<IBusiness<T>> businesses;
    private List<IAbility<T>> abilities;
    private List<InstanceHolder> instanceHolders;
    private List<ClassHolder> classHolders;

    @Bean
    @ConditionalOnMissingBean
    public IExtensionContext<T> registerExtensionContext(EasyExtensionConfigurationProperties properties) throws RegisterException, ProxyException {
        DefaultExtensionContext<T> extensionContext = new DefaultExtensionContext<>(
                properties.getEnableLog(),
                properties.effectiveUnknownBusinessPolicy(),
                properties.effectiveMultiMatchPolicy(),
                properties.getBusinessMatchOrder(),
                this.sessionManager != null ? this.sessionManager : new DefaultScopedSessionManager());
        if (this.businessMatchSelector != null) {
            logger.info("Using BusinessMatchSelector bean [{}] to pick one of several matching businesses",
                    this.businessMatchSelector.getClass().getName());
            extensionContext.setBusinessMatchSelector(this.businessMatchSelector);
        }
        if (this.interceptors != null) {
            for (ExtensionInterceptor interceptor : this.interceptors) {
                extensionContext.registerInterceptor(interceptor);
            }
        }
        // The interceptor beans are registered once every singleton exists, see registerInterceptorBeansWhenReady():
        // an interceptor may need the extension context itself, which is still being created here.
        this.createdContext = extensionContext;
        registerInterceptorBeansWhenReady();
        ExtensionContextRegisterByAnnHelper<T> helper = new ExtensionContextRegisterByAnnHelper<>(extensionContext);

        // if no extension point found, return empty context
        if (this.extensionPointHolders == null || this.extensionPointHolders.isEmpty()) {
            if (properties.getEnableLog()) {
                logger.info("No extension point found. Please check your configuration.");
            }
            return extensionContext;
        }

        registerExtensionPoint(helper);

        registerMatcherParamClass(helper);

        registerExtensionPointGroupImpl(helper);

        helper.doRegister();

        return extensionContext;
    }

    private void registerExtensionPoint(ExtensionContextRegisterByAnnHelper<T> helper) {
        for (ExtensionPointHolder holder : this.extensionPointHolders) {
            helper.addExtensionPointClasses(holder.getExtensionPointClass());
        }
    }

    @SuppressWarnings("unchecked")
    private void registerMatcherParamClass(ExtensionContextRegisterByAnnHelper<T> helper) throws RegisterParamException {
        if (this.classHolders == null || this.classHolders.isEmpty()) {
            throw new RegisterParamException("instance annotated with @MatcherParam not found");
        }

        List<Class<?>> matcherParamClasses = new ArrayList<>();
        for (ClassHolder classHolder : this.classHolders) {
            Class<?> clazz = classHolder.getClazz();
            if (clazz.isAnnotationPresent(MatcherParam.class)) {
                matcherParamClasses.add(clazz);
            }
        }
        if (matcherParamClasses.isEmpty()) {
            throw new RegisterParamException("instance annotated with @MatcherParam not found, classes scanned but none annotated with @MatcherParam");
        }
        if (matcherParamClasses.size() > 1) {
            throw new RegisterParamException("More than one instance annotated with @MatcherParam found: "
                    + matcherParamClasses.stream().map(Class::getName).sorted().toList());
        }
        helper.setMatcherParamClass((Class<T>) matcherParamClasses.get(0));
    }

    /**
     * register extension implementation, abilities, businesses
     */
    private void registerExtensionPointGroupImpl(ExtensionContextRegisterByAnnHelper<T> helper) throws RegisterParamException, ProxyException {
        List<Object> defaultImpls = new ArrayList<>();
        List<Matcher<T>> abilities = new ArrayList<>();
        List<Matcher<T>> businesses = new ArrayList<>();

        if (this.instanceHolders != null) {
            // register by Annotation
            for (InstanceHolder holder : this.instanceHolders) {
                collectByAnnotation(holder, defaultImpls, abilities, businesses);
            }
        }

        // register by bean
        if (this.extensionPointGroupImplementations != null) {
            defaultImpls.addAll(this.extensionPointGroupImplementations);
        }
        if (this.abilities != null && !this.abilities.isEmpty()) {
            abilities.addAll(this.abilities);
        }
        if (this.businesses != null && !this.businesses.isEmpty()) {
            businesses.addAll(this.businesses);
        }

        // register extension point default implementations: any number, each answering for the extension points it
        // implements. Whether every extension point ended up with one (or is mandatory) is checked once everything
        // is registered, by helper.doRegister().
        helper.addExtensionPointDefaultImplementations(defaultImpls.toArray());

        // register abilities
        for (Matcher<T> ability : abilities) {
            helper.addAbilities(ability);
        }

        // register businesses
        for (Matcher<T> business : businesses) {
            helper.addBusinesses(business);
        }
    }

    /**
     * Sort a scanned bean into default implementation, ability or business.
     * <p>
     * Annotations and implemented extension points are read from the bean's target class: the bean may be a Spring AOP
     * proxy, which carries neither. The bean itself, proxy included, is what gets registered, so Spring advice
     * (caching, transactions, ...) still applies when the framework invokes it.
     * </p>
     */
    private void collectByAnnotation(InstanceHolder holder, List<Object> defaultImpls, List<Matcher<T>> abilities,
                                     List<Matcher<T>> businesses) throws RegisterParamException, ProxyException {
        Object instance = holder.getInstance();
        Class<?> targetClass = holder.getTargetClass();
        if (targetClass.isAnnotationPresent(ExtensionPointDefaultImplementation.class)) {
            defaultImpls.add(AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(instance, targetClass));
        } else if (targetClass.isAnnotationPresent(Ability.class)) {
            abilities.add(AnnProxyConvertUtils.convertAnnAbilityToProxy(asMatcher(instance, targetClass, "@Ability"), targetClass));
        } else if (targetClass.isAnnotationPresent(Business.class)) {
            businesses.add(AnnProxyConvertUtils.convertAnnBusinessToProxy(asMatcher(instance, targetClass, "@Business"), targetClass));
        } else {
            // Scanned as an extension component, yet nothing to register it by. Skipping it silently would only
            // surface much later as "no business matched", so fail at startup instead.
            throw new RegisterParamException(String.format(
                    "class [%s] was scanned as an extension component but carries none of @Ability, @Business or "
                            + "@ExtensionPointDefaultImplementation (the annotations are read from the class itself, "
                            + "meta-annotations are not supported)", targetClass.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    private Matcher<T> asMatcher(Object instance, Class<?> targetClass, String annotation) throws RegisterParamException {
        if (instance instanceof Matcher<?> matcher) {
            return (Matcher<T>) matcher;
        }
        throw new RegisterParamException(String.format(
                "instance annotated with %s should implement Matcher interface, but [%s] does not", annotation, targetClass.getName()));
    }

    @Autowired(required = false)
    public void setAbilities(List<IAbility<T>> abilities) {
        this.abilities = abilities;
    }

    @Autowired(required = false)
    public void setBusinesses(List<IBusiness<T>> businesses) {
        this.businesses = businesses;
    }

    /**
     * Set the one default implementation. Kept for callers that configure the auto-configuration by hand;
     * the container uses {@link #setExtensionPointGroupImplementationBeans(ObjectProvider)}.
     */
    public void setExtensionPointGroupImplementation(IExtensionPointGroupDefaultImplementation<T> extensionPointGroupImplementation) {
        this.extensionPointGroupImplementations = extensionPointGroupImplementation == null ? null : List.of(extensionPointGroupImplementation);
    }

    /**
     * Set the default implementations that are beans of their own (as opposed to classes annotated with
     * {@code @ExtensionPointDefaultImplementation}), for callers that configure the auto-configuration by hand.
     */
    public void setExtensionPointGroupImplementations(List<IExtensionPointGroupDefaultImplementation<T>> extensionPointGroupImplementations) {
        this.extensionPointGroupImplementations = extensionPointGroupImplementations;
    }

    /**
     * Default implementations that are beans of their own; there may be several, each for its own extension points.
     * <p>
     * If one of several is {@code @Primary}, it alone is the default implementation, as it was before 3.4 when a
     * single bean was injected and {@code @Primary} chose it.
     * </p>
     */
    @Autowired(required = false)
    public void setExtensionPointGroupImplementationBeans(ObjectProvider<IExtensionPointGroupDefaultImplementation<T>> beans) {
        List<IExtensionPointGroupDefaultImplementation<T>> all = beans.orderedStream().toList();
        if (all.size() > 1) {
            IExtensionPointGroupDefaultImplementation<T> primary = beans.getIfUnique();
            if (primary != null) {
                logger.info("{} extension point default implementation beans found, [{}] is @Primary: it is the default "
                        + "implementation, the others are ignored", all.size(), primary.getClass().getName());
                all = List.of(primary);
            }
        }
        this.extensionPointGroupImplementations = all;
    }

    /**
     * Where the resolved chain of the current request is kept; a thread local unless a bean says otherwise.
     * Set by hand, or by {@link #setSessionManagerBeans(ObjectProvider)}.
     */
    public void setSessionManager(IScopedSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * The {@link IScopedSessionManager} bean, if there is one. With several (and none {@code @Primary}) it is not
     * clear which one is meant for this context, so none is used.
     */
    @Autowired(required = false)
    public void setSessionManagerBeans(ObjectProvider<IScopedSessionManager> beans) {
        this.sessionManager = uniqueBean(beans, "IScopedSessionManager");
    }

    /**
     * How one business is picked when several match (and the policy allows it).
     * Set by hand, or by {@link #setBusinessMatchSelectorBeans(ObjectProvider)}.
     */
    public void setBusinessMatchSelector(BusinessMatchSelector<T> businessMatchSelector) {
        this.businessMatchSelector = businessMatchSelector;
    }

    /**
     * The {@link BusinessMatchSelector} bean, if there is one. With several (and none {@code @Primary}) it is not
     * clear which one is meant for this context, so none is used.
     */
    @Autowired(required = false)
    public void setBusinessMatchSelectorBeans(ObjectProvider<BusinessMatchSelector<T>> beans) {
        this.businessMatchSelector = uniqueBean(beans, "BusinessMatchSelector");
    }

    /**
     * The one bean (or the {@code @Primary} one among several), {@code null} if there is none or it is ambiguous.
     */
    private static <B> B uniqueBean(ObjectProvider<B> beans, String type) {
        B unique = beans.getIfUnique();
        if (unique == null) {
            long count = beans.stream().count();
            if (count > 1) {
                logger.warn("{} {} beans found and none is @Primary: it is not clear which one is meant for the extension "
                        + "context, none is used. Mark one @Primary, or call the setter of the extension context yourself.", count, type);
            }
        }
        return unique;
    }

    /**
     * Interceptors around every call to an extension implementation that are registered when the extension context is
     * created, for callers that configure the auto-configuration by hand; the container uses
     * {@link #setInterceptorBeans(ObjectProvider)}.
     */
    public void setInterceptors(List<ExtensionInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * The {@link ExtensionInterceptor} beans, in {@code @Order} order. They are not resolved here: an interceptor that
     * needs the extension context (to ask which business the request resolved to, for example) would make the
     * context depend on itself. See {@link #extensionInterceptorRegistration()}.
     *
     * @since 3.4
     */
    @Autowired(required = false)
    public void setInterceptorBeans(ObjectProvider<ExtensionInterceptor> interceptorBeans) {
        this.interceptorBeans = interceptorBeans;
    }

    /**
     * Registers the {@link ExtensionInterceptor} beans with the extension context once every singleton exists, so an
     * interceptor may depend on the extension context like any other bean. A call to an extension point made while the
     * beans are still being created (a {@code @PostConstruct}) is therefore not intercepted. Where the context is
     * created later (lazy initialization), the beans are registered when it is.
     *
     * @since 3.4
     */
    @Bean
    @Lazy(false)
    public SmartInitializingSingleton extensionInterceptorRegistration() {
        return () -> {
            this.singletonsInstantiated = true;
            registerInterceptorBeansWhenReady();
        };
    }

    private void registerInterceptorBeansWhenReady() {
        DefaultExtensionContext<T> context = this.createdContext;
        ObjectProvider<ExtensionInterceptor> beans = this.interceptorBeans;
        if (context == null || beans == null || !this.singletonsInstantiated
                || !this.interceptorBeansRegistered.compareAndSet(false, true)) {
            return;
        }
        // in @Order order, the first being the outermost
        for (ExtensionInterceptor interceptor : beans.orderedStream().toList()) {
            try {
                context.registerInterceptor(interceptor);
            } catch (RegisterException e) {
                throw new IllegalStateException("could not register interceptor " + interceptor.getClass().getName(), e);
            }
        }
    }

    @Autowired(required = false)
    public void setExtensionPointHolders(List<ExtensionPointHolder> extensionPointHolders) {
        this.extensionPointHolders = extensionPointHolders;
    }

    @Autowired(required = false)
    public void setInstanceHolders(List<InstanceHolder> instanceHolders) {
        this.instanceHolders = instanceHolders;
    }

    @Autowired(required = false)
    public void setClassHolders(List<ClassHolder> classHolders) {
        this.classHolders = classHolders;
    }
}
