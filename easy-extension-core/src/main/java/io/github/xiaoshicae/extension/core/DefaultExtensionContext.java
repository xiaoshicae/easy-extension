package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.OrderedCodeBusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.extension.DefaultExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultExtensionContext<T> implements IExtensionContext<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExtensionContext.class);
    static final String LOG_PREFIX = "[Easy Extension]";
    static final String DEFAULT_SCOPE = "__easy__extension__default__scope__";
    private final static String EASY_EXTENSION_DEFAULT_SCOPE = DEFAULT_SCOPE;

    /**
     * Whether logger enabled.
     */
    private final boolean enableLogger;

    /**
     * Strategy used to pick one business when multiple match. Defaults to the
     * code-ordered selector built from the configured business match order, which reproduces the historical
     * behaviour of {@code easy-extension.business-match-order}.
     */
    private volatile BusinessMatchSelector<T> businessMatchSelector;

    /**
     * Scoped session manager: where the resolved chain of the current request is kept.
     */
    private final IScopedSessionManager session;

    /**
     * Ability manager.
     */
    private final IAbilityManager<T> abilityManager = new DefaultAbilityManager<>();

    /**
     * Business manager.
     */
    private final IBusinessManager<T> businessManager = new DefaultBusinessManager<>();

    /**
     * Extension point group implementation manager.
     *
     * <p>Mange the extension point group default implementation, i.e. ability, business, default implementation ...</p>
     */
    private final IExtensionPointGroupImplementationManager<T> extensionPointGroupImplementationManager = new DefaultExtensionPointGroupImplementationManager<>();

    /**
     * All extension point classes.
     */
    private final Set<Class<?>> allExtensionPointClasses = Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * Matcher param class.
     */
    private Class<T> matcherParamClass;

    /**
     * Extension point default implementations: the fallback at the end of every resolved chain.
     */
    private final DefaultsRegistry<T> defaults = new DefaultsRegistry<>();

    /**
     * Decides who a request is (business, abilities, defaults) as a resolved chain.
     */
    private final ChainResolver<T> resolver;

    /**
     * ThreadLocal storage for the most recent resolve trace.
     */
    private final ThreadLocal<ResolveTrace> lastResolveTrace = new ThreadLocal<>();

    /**
     * The hot path: finds the implementation to call, wrapped for the registered interceptors if there are any.
     */
    private final ExtensionLookup<T> lookup;

    private record CachedRegistryVersion(long modCount, String version) {
    }

    /**
     * Counts registrations, to know when the cached registry version is stale.
     */
    private final AtomicLong registryModCount = new AtomicLong();
    private volatile CachedRegistryVersion cachedRegistryVersion;

    public DefaultExtensionContext() {
        this(false, false, List.of());
    }

    /**
     * @param enableLogger        whether to log the resolution of every request
     * @param matchBusinessStrict {@code true} for exactly one matching business per request, rejecting both "none"
     *                            and "several"; {@code false} for falling back to the default implementations when
     *                            none matches and selecting one when several do. Equivalent to
     *                            {@link UnknownBusinessPolicy} and {@link MultiMatchPolicy} being both
     *                            {@code REJECT}, or {@code DEFAULT} and {@code SELECT}.
     */
    public DefaultExtensionContext(boolean enableLogger, boolean matchBusinessStrict) {
        this(enableLogger, matchBusinessStrict, List.of());
    }

    /**
     * @param businessMatchOrder codes of businesses in order of preference, for when several match
     * @see #DefaultExtensionContext(boolean, boolean)
     */
    public DefaultExtensionContext(boolean enableLogger, boolean matchBusinessStrict, List<String> businessMatchOrder) {
        this(enableLogger,
                matchBusinessStrict ? UnknownBusinessPolicy.REJECT : UnknownBusinessPolicy.DEFAULT,
                matchBusinessStrict ? MultiMatchPolicy.REJECT : MultiMatchPolicy.SELECT,
                businessMatchOrder);
    }

    /**
     * @param enableLogger          whether to log the resolution of every request
     * @param unknownBusinessPolicy what to do when no business matches
     * @param multiMatchPolicy      what to do when several businesses match
     * @param businessMatchOrder    codes of businesses in order of preference, for when several match and the
     *                              policy lets the selector choose
     * @since 3.4
     */
    public DefaultExtensionContext(boolean enableLogger, UnknownBusinessPolicy unknownBusinessPolicy,
                                   MultiMatchPolicy multiMatchPolicy, List<String> businessMatchOrder) {
        this(enableLogger, unknownBusinessPolicy, multiMatchPolicy, businessMatchOrder, new DefaultScopedSessionManager());
    }

    /**
     * @param sessionManager where the resolved chain of the current request is kept. The default one keeps it in a
     *                       thread local; supply another to hold it somewhere else (a store that follows the
     *                       request across threads, a reactive context, ...)
     * @see #DefaultExtensionContext(boolean, UnknownBusinessPolicy, MultiMatchPolicy, List)
     * @since 3.4
     */
    public DefaultExtensionContext(boolean enableLogger, UnknownBusinessPolicy unknownBusinessPolicy,
                                   MultiMatchPolicy multiMatchPolicy, List<String> businessMatchOrder,
                                   IScopedSessionManager sessionManager) {
        this.enableLogger = enableLogger;
        this.session = Objects.requireNonNull(sessionManager, "sessionManager should not be null");
        this.businessMatchSelector = new OrderedCodeBusinessMatchSelector<>(businessMatchOrder != null ? businessMatchOrder : List.of());
        this.lookup = new ExtensionLookup<>(session, extensionPointGroupImplementationManager, enableLogger);
        this.resolver = new ChainResolver<>(businessManager, abilityManager, defaults,
                Objects.requireNonNull(unknownBusinessPolicy, "unknownBusinessPolicy should not be null"),
                Objects.requireNonNull(multiMatchPolicy, "multiMatchPolicy should not be null"),
                () -> this.businessMatchSelector, enableLogger);
    }

    /**
     * Replace the default code-ordered selector with a custom strategy
     * (e.g. grayscale-aware or tenant-hierarchy-aware).
     */
    public void setBusinessMatchSelector(BusinessMatchSelector<T> selector) {
        if (selector == null) {
            throw new IllegalArgumentException("BusinessMatchSelector should not be null");
        }
        this.businessMatchSelector = selector;
    }

    @Override
    public void registerExtensionPoint(Class<?> clazz) throws RegisterException {
        if (clazz == null) {
            throw new RegisterParamException("clazz should not be null");
        }
        if (!clazz.isInterface()) {
            throw new RegisterParamException("clazz should be an interface type");
        }

        synchronized (allExtensionPointClasses) {
            if (allExtensionPointClasses.contains(clazz)) {
                throw new RegisterDuplicateException(String.format("class [%s] already registered", clazz.getName()));
            }
            allExtensionPointClasses.add(clazz);
        }
        registryChanged();

        if (enableLogger) {
            logger.info("{} register extension point class: [{}]", LOG_PREFIX, clazz.getSimpleName());
        }
    }

    @Override
    public void registerMatcherParamClass(Class<T> matcherParamClass) throws RegisterException {
        if (matcherParamClass == null) {
            throw new RegisterParamException("matcher param class should not be null");
        }

        if (this.matcherParamClass != null) {
            throw new RegisterDuplicateException("matcher param class already registered");
        }

        this.matcherParamClass = matcherParamClass;

        if (enableLogger) {
            logger.info("{} register matcher param class: [{}]", LOG_PREFIX, matcherParamClass.getSimpleName());
        }
    }

    @Override
    public void registerExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException {
        if (instance == null) {
            throw new RegisterParamException("extension point default implementation should not be null");
        }

        defaults.register(instance, () -> {
            if (!defaults.isEmpty()) {
                throw new RegisterDuplicateException("extension point default implementation already registered");
            }

            List<Class<?>> mustImplementExtensionPoints = instance.implementExtensionPoints();
            List<Class<?>> notImplementClasses;
            synchronized (allExtensionPointClasses) {
                notImplementClasses = allExtensionPointClasses.stream()
                        .filter(clazz -> !mustImplementExtensionPoints.contains(clazz))
                        .toList();
            }
            if (!notImplementClasses.isEmpty()) {
                throw new RegisterParamException(String.format("extension point default implementation should implement all extension point, but in fact, it has not implement [%s]", notImplementClasses.stream().map(Class::getName).collect(Collectors.joining(", "))));
            }

            extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(instance);
        });
        registryChanged();

        if (enableLogger) {
            logger.info("{} register extension point default implementation: [{}]", LOG_PREFIX, simpleNameOf(instance));
        }
    }

    @Override
    public void addExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException {
        if (instance == null) {
            throw new RegisterParamException("extension point default implementation should not be null");
        }

        defaults.register(instance, () -> {
            defaults.checkAdditional(instance);
            for (Class<?> implExtClass : instance.implementExtensionPoints()) {
                if (allExtensionPointClasses.contains(implExtClass)) {
                    continue;
                }
                if (instance instanceof IProxy<?>) {
                    // Found by annotation. Releases before 3.4 did not hold it against a default implementation that
                    // it implements something nobody registered (an interface of a module that is not scanned).
                    logger.warn("{} default implementation [{}] implements extension point [{}], which is not registered: "
                            + "it is left out", LOG_PREFIX, DefaultsRegistry.describe(instance), implExtClass.getName());
                    continue;
                }
                throw new RegisterException(String.format("extension point [%s] not registered", implExtClass.getName()));
            }
            extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(instance);
        });
        registryChanged();

        if (enableLogger) {
            logger.info("{} register extension point default implementation: [{}]", LOG_PREFIX, simpleNameOf(instance));
        }
    }

    /**
     * Every extension point an ability or a business implements must be registered.
     * <p>
     * An implementation that the framework proxied (found by annotation) is judged by what its class declares
     * itself. An extension point it merely inherits, from a superclass or a super-interface, may belong to a module
     * that is not scanned, and releases before 3.4 did not look at inherited extension points at all: it is left
     * out when it is not registered, and said so when it is (the routing of such a call differs from 3.3).
     * </p>
     */
    private void requireRegistered(String kind, String code, Object implementation, List<Class<?>> extensionPoints) throws RegisterException {
        for (Class<?> implExtClass : extensionPoints) {
            boolean onlyInherited = isOnlyInherited(implementation, implExtClass);
            if (!allExtensionPointClasses.contains(implExtClass)) {
                if (onlyInherited) {
                    logger.debug("{} {} [{}] inherits extension point [{}], which is not registered: it is left out",
                            LOG_PREFIX, kind, code, implExtClass.getName());
                    continue;
                }
                throw new RegisterException(String.format("extension point [%s] not registered", implExtClass.getName()));
            }
            if (onlyInherited && enableLogger) {
                logger.info("{} {} [{}] answers for extension point [{}] through a superclass or a super-interface "
                        + "(releases before 3.4 did not count those)", LOG_PREFIX, kind, code, implExtClass.getName());
            }
        }
    }

    private static boolean isOnlyInherited(Object implementation, Class<?> extensionPoint) {
        if (!(implementation instanceof IProxy<?> proxy)) {
            return false;
        }
        for (Class<?> declared : proxy.getTargetClass().getInterfaces()) {
            if (declared == extensionPoint) {
                return false;
            }
        }
        return true;
    }

    private static String simpleNameOf(Object instance) {
        return instance instanceof IProxy<?> proxy ? proxy.getTargetClass().getSimpleName() : instance.getClass().getSimpleName();
    }

    @Override
    public void validateRegistration() throws RegisterException {
        Set<Class<?>> extensionPoints;
        synchronized (allExtensionPointClasses) {
            extensionPoints = new LinkedHashSet<>(allExtensionPointClasses);
        }
        List<Class<?>> withoutDefault = defaults.uncovered(extensionPoints).stream()
                .filter(clazz -> !DefaultsRegistry.isMandatory(clazz))
                .toList();
        if (withoutDefault.isEmpty()) {
            return;
        }
        if (defaults.isEmpty()) {
            throw new RegisterParamException("extension point default implementation not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist");
        }
        throw new RegisterParamException(String.format(
                "extension point default implementation should implement all extension point, but in fact, it has not implement [%s]"
                        + " (an extension point without a sensible default can be marked @ExtensionPoint(mandatory = true))",
                withoutDefault.stream().map(Class::getName).collect(Collectors.joining(", "))));
    }

    @Override
    public void registerAbility(IAbility<T> ability) throws RegisterException {
        if (ability == null) {
            throw new RegisterParamException("ability should not be null");
        }

        requireRegistered("ability", ability.code(), ability, ability.implementExtensionPoints());

        abilityManager.registerAbility(ability);
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(ability);
        registryChanged();

        if (enableLogger) {
            logger.info("{} register ability: [{}]", LOG_PREFIX, ability.code());
        }
    }

    @Override
    public void registerBusiness(IBusiness<T> business) throws RegisterException {
        if (business == null) {
            throw new RegisterParamException("business should not be null");
        }

        requireRegistered("business", business.code(), business, business.implementExtensionPoints());

        Set<String> codeSet = new HashSet<>();
        Set<Integer> prioritySet = new HashSet<>();

        // Include business's own priority in the set for unified conflict detection
        if (business.priority() != null) {
            prioritySet.add(business.priority());
        }

        if (business.usedAbilities() != null) {
            for (UsedAbility usedAbility : business.usedAbilities()) {
                try {
                    abilityManager.getAbility(usedAbility.code());
                } catch (QueryException e) {
                    throw new RegisterException(String.format("business [%s] used ability [%s] not found", business.code(), usedAbility.code()));
                }

                if (codeSet.contains(usedAbility.code())) {
                    throw new RegisterException(String.format("business [%s] used ability [%s] duplicate", business.code(), usedAbility.code()));
                }

                if (prioritySet.contains(usedAbility.priority())) {
                    throw new RegisterException(String.format("business [%s] used ability with priority [%d] conflict", business.code(), usedAbility.priority()));
                }

                codeSet.add(usedAbility.code());
                prioritySet.add(usedAbility.priority());
            }
        }

        // Validate ability requires/excludes constraints
        validateAbilityConstraints(business.code(), codeSet);

        businessManager.registerBusiness(business);
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(business);
        registryChanged();

        if (enableLogger) {
            logger.info("{} register business: [{}]", LOG_PREFIX, business.code());
        }
    }

    @Override
    public void registerInterceptor(ExtensionInterceptor interceptor) throws RegisterException {
        if (interceptor == null) {
            throw new RegisterParamException("interceptor should not be null");
        }
        lookup.addInterceptor(interceptor);
    }

    /**
     * Validate ability requires/excludes constraints for a business.
     */
    private void validateAbilityConstraints(String businessCode, Set<String> usedAbilityCodes) throws RegisterException {
        for (String abilityCode : usedAbilityCodes) {
            IAbility<T> ability;
            try {
                ability = abilityManager.getAbility(abilityCode);
            } catch (QueryException e) {
                continue;
            }

            // Resolve @Ability annotation from the actual implementation class
            Class<?> abilityClass = ability instanceof IProxy<?> proxy
                    ? proxy.getTargetClass() : ability.getClass();
            io.github.xiaoshicae.extension.core.annotation.Ability ann =
                    abilityClass.getAnnotation(io.github.xiaoshicae.extension.core.annotation.Ability.class);
            if (ann == null) continue;

            // Check requires
            for (String required : ann.requires()) {
                if (!usedAbilityCodes.contains(required)) {
                    throw new RegisterException(String.format(
                            "business [%s] mounts ability [%s] which requires ability [%s], but [%s] is not mounted",
                            businessCode, abilityCode, required, required));
                }
            }

            // Check excludes
            for (String excluded : ann.excludes()) {
                if (usedAbilityCodes.contains(excluded)) {
                    throw new RegisterException(String.format(
                            "business [%s] mounts ability [%s] which excludes ability [%s], but both are mounted",
                            businessCode, abilityCode, excluded));
                }
            }
        }
    }

    /**
     * A registration happened: the cached registry version is stale.
     */
    private void registryChanged() {
        registryModCount.incrementAndGet();
    }

    @Override
    public String registryVersion() {
        long modCount = registryModCount.get();
        CachedRegistryVersion cached = cachedRegistryVersion;
        if (cached != null && cached.modCount() == modCount) {
            return cached.version();
        }

        Set<Class<?>> extensionPoints;
        synchronized (allExtensionPointClasses) {
            extensionPoints = new LinkedHashSet<>(allExtensionPointClasses);
        }
        String version = RegistryFingerprint.of(extensionPoints, abilityManager.listAllAbilities(),
                businessManager.listAllBusinesses(), defaults.list());
        if (registryModCount.get() == modCount) {
            // nothing was registered meanwhile; otherwise the next call computes it again
            cachedRegistryVersion = new CachedRegistryVersion(modCount, version);
        }
        return version;
    }

    @Override
    public List<Class<?>> listAllExtensionPoint() {
        synchronized (allExtensionPointClasses) {
            return List.copyOf(allExtensionPointClasses);
        }
    }

    @Override
    public Class<T> getMatcherParamClass() {
        return matcherParamClass;
    }

    @Override
    public IExtensionPointGroupDefaultImplementation<T> getExtensionPointDefaultImplementation() {
        return defaults.first();
    }

    @Override
    public List<IExtensionPointGroupDefaultImplementation<T>> listExtensionPointDefaultImplementations() {
        return defaults.list();
    }

    @Override
    public List<IAbility<T>> listAllAbility() {
        return abilityManager.listAllAbilities();
    }

    @Override
    public List<IBusiness<T>> listAllBusiness() {
        return businessManager.listAllBusinesses();
    }

    @Override
    public void initSession(T param) throws SessionException {
        initSession(EASY_EXTENSION_DEFAULT_SCOPE, param);
    }

    @Override
    public void initSession(String scope, T param) throws SessionException {
        if (scope == null) {
            throw new SessionParamException("scope should not be null");
        }
        long startTime = enableLogger ? System.currentTimeMillis() : 0L;
        boolean defaultScope = EASY_EXTENSION_DEFAULT_SCOPE.equals(scope);

        if (defaultScope && enableLogger && session.hasScopedSession(EASY_EXTENSION_DEFAULT_SCOPE)) {
            logger.warn("{} session already initialized, this call will override previous session data", LOG_PREFIX);
        }
        session.removeScopedSession(scope);

        if (enableLogger) {
            if (defaultScope) {
                logger.info("{} session init start", LOG_PREFIX);
            } else {
                logger.info("{} session with scope: [{}], init start", LOG_PREFIX, scope);
            }
        }

        ChainResolver.Resolution resolution;
        try {
            resolution = resolver.resolve(scope, defaultScope, param, registryVersion());
        } catch (SessionParamException e) {
            // already says which scope it is about
            throw e;
        } catch (SessionException e) {
            if (defaultScope) throw e;
            throw new SessionException(String.format("scope [%s], %s", scope, e.getMessage()), e);
        }
        session.bindScopedChain(scope, resolution.chain());
        lastResolveTrace.set(resolution.trace());

        if (enableLogger) {
            long cost = System.currentTimeMillis() - startTime;
            if (defaultScope) {
                logger.info("{} session init completed, time cost: [{} ms]", LOG_PREFIX, cost);
            } else {
                logger.info("{} session with scope: [{}], init completed, time cost: [{} ms]", LOG_PREFIX, scope, cost);
            }
        }
    }

    /**
     * How many distinct combinations of multiply-matching businesses were warned about; for tests.
     */
    int multiMatchWarningCount() {
        return resolver.multiMatchWarningCount();
    }

    @Override
    public ResolvedChain resolve(T param) throws SessionException {
        return resolver.resolve(EASY_EXTENSION_DEFAULT_SCOPE, true, param, registryVersion()).chain();
    }

    @Override
    public ResolvedChain currentChain() {
        return currentChain(EASY_EXTENSION_DEFAULT_SCOPE);
    }

    @Override
    public ResolvedChain currentChain(String scope) {
        return session.getScopedChain(scope);
    }

    @Override
    public void bind(ResolvedChain chain) throws SessionException {
        bind(EASY_EXTENSION_DEFAULT_SCOPE, chain);
    }

    @Override
    public void bind(String scope, ResolvedChain chain) throws SessionException {
        if (scope == null) {
            throw new SessionParamException("scope should not be null");
        }
        if (chain == null) {
            throw new SessionParamException("chain should not be null");
        }
        verifyChainFitsRegistry(chain);

        session.bindScopedChain(scope, chain);
        lastResolveTrace.set(traceOf(scope, chain));

        if (enableLogger) {
            logger.info("{} session {}bound to a resolved chain: {}", LOG_PREFIX,
                    EASY_EXTENSION_DEFAULT_SCOPE.equals(scope) ? "" : "with scope: [" + scope + "], ", chain);
        }
    }

    private void verifyChainFitsRegistry(ResolvedChain chain) throws SessionException {
        String version = registryVersion();
        if (!version.equals(chain.registryVersion())) {
            throw new SessionException(String.format(
                    "chain was resolved against a different registry (chain version [%s], current version [%s]), resolve it again",
                    chain.registryVersion(), version));
        }
        for (ResolutionEntry entry : chain.entries()) {
            if (!isKnown(entry)) {
                throw new SessionException(String.format("chain refers to unknown %s [%s]", entry.type().label(), entry.code()));
            }
        }
    }

    private boolean isKnown(ResolutionEntry entry) {
        try {
            return switch (entry.type()) {
                case BUSINESS -> businessManager.findBusiness(entry.code()) != null;
                case ABILITY -> abilityManager.findAbility(entry.code()) != null;
                case DEFAULT -> defaults.hasCode(entry.code());
            };
        } catch (QueryException e) {
            return false;
        }
    }

    /**
     * What a bound chain amounts to as a trace: the chain says what was resolved, not why (which abilities
     * were skipped and how long it took are not part of it).
     */
    private ResolveTrace traceOf(String scope, ResolvedChain chain) {
        ResolveTrace.Builder builder = ResolveTrace.builder(scope);
        for (ResolutionEntry entry : chain.entries()) {
            if (entry.type() == EntryType.BUSINESS) {
                builder.matchedBusiness(entry.code(), entry.priority());
            } else if (entry.type() == EntryType.ABILITY) {
                builder.abilityMatched(entry.code(), entry.priority());
            } else {
                builder.defaultImpl(entry.code(), entry.priority());
            }
        }
        return builder.build();
    }

    @Override
    public void removeSession() {
        session.removeAllSession();
        lastResolveTrace.remove();
        if (enableLogger) {
            logger.info("{} all session (include scoped session) has been removed", LOG_PREFIX);
        }
    }

    @Override
    public void removeSession(String scope) {
        session.removeScopedSession(scope);
        ResolveTrace trace = lastResolveTrace.get();
        if (trace != null && Objects.equals(trace.getScope(), scope)) {
            lastResolveTrace.remove();
        }
        if (enableLogger) {
            logger.info("{} session with scope: [{}] has been removed", LOG_PREFIX, scope);
        }
    }

    @Override
    public ResolveTrace getLastResolveTrace() {
        return lastResolveTrace.get();
    }

    @Override
    public <E> ExtensionExplanation<E> explain(Class<E> extensionPointType) {
        return explainScoped(EASY_EXTENSION_DEFAULT_SCOPE, extensionPointType);
    }

    @Override
    public <E> ExtensionExplanation<E> explainScoped(String scope, Class<E> extensionPointType) {
        if (extensionPointType == null) {
            throw new IllegalArgumentException("extensionPointType should not be null");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope should not be null");
        }

        List<ResolutionEntry> chain = resolutionChainOf(scope);
        List<ExtensionExplanation.Candidate> candidates = new ArrayList<>(chain.size());
        ExtensionExplanation.Candidate selected = null;
        for (ResolutionEntry entry : chain) {
            Class<?> implClass = null;
            boolean implemented = false;
            try {
                E instance = extensionPointGroupImplementationManager
                        .findExtensionPointImplementationInstance(extensionPointType, entry.code());
                if (instance != null) {
                    implemented = true;
                    implClass = instance instanceof IProxy<?> p
                            ? p.getTargetClass()
                            : instance.getClass();
                }
            } catch (QueryException ignored) {
                // not a usable extension point type for this entry; keep implemented=false
            }
            ExtensionExplanation.Candidate c = new ExtensionExplanation.Candidate(
                    entry.code(), entry.priority(), entry.type(), implClass, implemented);
            candidates.add(c);
            if (selected == null && implemented) {
                selected = c;
            }
        }
        return new ExtensionExplanation<>(extensionPointType, scope, candidates, selected);
    }

    /**
     * The chain bound to the scope, as entries; empty if the scope has none.
     */
    private List<ResolutionEntry> resolutionChainOf(String scope) {
        try {
            ResolvedChain bound = session.getScopedChain(scope);
            return bound == null ? List.of() : bound.entries();
        } catch (UnsupportedOperationException e) {
            // a session manager that does not keep chains: the trace of the latest resolution is all there is
            ResolveTrace trace = lastResolveTrace.get();
            return trace != null && Objects.equals(trace.getScope(), scope)
                    ? trace.getResolutionChain()
                    : List.of();
        }
    }

    @Override
    public <E> E getFirstMatchedExtension(Class<E> extensionType) throws QueryException {
        return getFirstMatchedExtension(EASY_EXTENSION_DEFAULT_SCOPE, extensionType);
    }

    @Override
    public <E> List<E> getAllMatchedExtension(Class<E> extensionType) throws QueryException {
        return getAllMatchedExtension(EASY_EXTENSION_DEFAULT_SCOPE, extensionType);
    }

    @Override
    public <E> E getFirstMatchedExtension(String scope, Class<E> extensionType) throws QueryException {
        return lookup.first(scope, extensionType);
    }

    @Override
    public <E> List<E> getAllMatchedExtension(String scope, Class<E> extensionType) throws QueryException {
        return lookup.all(scope, extensionType);
    }

    @Override
    public <E, R> R invoke(Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        return invoke(EASY_EXTENSION_DEFAULT_SCOPE, extensionType, invoker);
    }

    @Override
    public <E, R> List<R> invokeAll(Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        return invokeAll(EASY_EXTENSION_DEFAULT_SCOPE, extensionType, invoker);
    }

    @Override
    public <E, R> R invokeReduce(Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        return invokeReduce(EASY_EXTENSION_DEFAULT_SCOPE, extensionType, invoker, identity, accumulator);
    }

    @Override
    public <E, R> R invoke(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        E extension;
        try {
            extension = getFirstMatchedExtension(scope, extensionType);
        } catch (QueryException e) {
            throw new InvokeException(invokeErrorPrefix("invoke", scope) + e.getMessage(), e);
        }
        return invoker.apply(extension);
    }

    @Override
    public <E, R> List<R> invokeAll(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        List<E> extensions;
        try {
            extensions = getAllMatchedExtension(scope, extensionType);
        } catch (QueryException e) {
            throw new InvokeException(invokeErrorPrefix("invokeAll", scope) + e.getMessage(), e);
        }
        List<R> resList = new ArrayList<>();
        for (E extension : extensions) {
            resList.add(invoker.apply(extension));
        }
        return resList;
    }

    @Override
    public <E, R> R invokeReduce(String scope, Class<E> extensionType, Function<E, R> invoker, R identity, BinaryOperator<R> accumulator) {
        List<E> extensions;
        try {
            extensions = getAllMatchedExtension(scope, extensionType);
        } catch (QueryException e) {
            throw new InvokeException(invokeErrorPrefix("invokeReduce", scope) + e.getMessage(), e);
        }
        R result = identity;
        for (E extension : extensions) {
            result = accumulator.apply(result, invoker.apply(extension));
        }
        return result;
    }

    private static String invokeErrorPrefix(String op, String scope) {
        return EASY_EXTENSION_DEFAULT_SCOPE.equals(scope)
                ? op + " failed, "
                : "scope " + scope + " " + op + " failed, ";
    }
}
