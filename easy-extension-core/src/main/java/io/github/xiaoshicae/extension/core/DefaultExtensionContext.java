package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
import io.github.xiaoshicae.extension.core.business.OrderedCodeBusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.extension.DefaultExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultExtensionContext<T> implements IExtensionContext<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExtensionContext.class);
    private static final String LOG_PREFIX = "[Easy Extension]";
    private final static String EASY_EXTENSION_DEFAULT_SCOPE = "__easy__extension__default__scope__";

    /**
     * Whether to match business strict.
     * <br>
     * <p>If strict, there is one and only one business should be matched.</p>
     * <p>If not strict, use extension default implementation when no business matched, use first matched business.</p>
     */
    private final boolean matchBusinessStrict;

    /**
     * Whether logger enabled.
     */
    private final boolean enableLogger;

    /**
     * Configured business match order. When multiple businesses match (non-strict mode),
     * the business appearing first in this list wins. Businesses not in the list
     * are ordered after listed ones by registration order.
     * <p>
     * Retained for backward compatibility; at runtime it is wrapped in an
     * {@link OrderedCodeBusinessMatchSelector} unless a custom {@link BusinessMatchSelector}
     * is supplied via {@link #setBusinessMatchSelector(BusinessMatchSelector)}.
     * </p>
     */
    private final List<String> businessMatchOrder;

    /**
     * Strategy used to pick one business when multiple match. Defaults to the
     * code-ordered selector built from {@link #businessMatchOrder}.
     */
    private volatile BusinessMatchSelector<T> businessMatchSelector;

    /**
     * Scoped session manager.
     */
    private final IScopedSessionManager session = new DefaultScopedSessionManager();

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
     * Extension point default implementation.
     */
    private IExtensionPointGroupDefaultImplementation<T> extensionPointDefaultImplementation;

    /**
     * ThreadLocal storage for the most recent resolve trace.
     */
    private final ThreadLocal<ResolveTrace> lastResolveTrace = new ThreadLocal<>();

    /**
     * ThreadLocal storage for the resolve trace of each initialized scope, so that
     * {@link #explainScoped(String, Class)} works for every scope, not only the last one.
     */
    private final ThreadLocal<Map<String, ResolveTrace>> scopedResolveTraces = ThreadLocal.withInitial(HashMap::new);

    public DefaultExtensionContext() {
        this(false, false, List.of());
    }

    public DefaultExtensionContext(boolean enableLogger, boolean matchBusinessStrict) {
        this(enableLogger, matchBusinessStrict, List.of());
    }

    public DefaultExtensionContext(boolean enableLogger, boolean matchBusinessStrict, List<String> businessMatchOrder) {
        this.enableLogger = enableLogger;
        this.matchBusinessStrict = matchBusinessStrict;
        this.businessMatchOrder = businessMatchOrder != null ? businessMatchOrder : List.of();
        this.businessMatchSelector = new OrderedCodeBusinessMatchSelector<>(this.businessMatchOrder);
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
        if (extensionPointDefaultImplementation != null) {
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

        extensionPointDefaultImplementation = instance;
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(instance);

        if (enableLogger) {
            String name = instance instanceof IProxy<?> proxy ? proxy.getInstance().getClass().getSimpleName() : instance.getClass().getSimpleName();
            logger.info("{} register extension point default implementation: [{}]", LOG_PREFIX, name);
        }
    }

    @Override
    public void registerAbility(IAbility<T> ability) throws RegisterException {
        if (ability == null) {
            throw new RegisterParamException("ability should not be null");
        }

        for (Class<?> implExtClass : ability.implementExtensionPoints()) {
            if (!allExtensionPointClasses.contains(implExtClass)) {
                throw new RegisterException(String.format("extension point [%s] not registered", implExtClass.getName()));
            }
        }

        abilityManager.registerAbility(ability);
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(ability);

        if (enableLogger) {
            logger.info("{} register ability: [{}]", LOG_PREFIX, ability.code());
        }
    }

    @Override
    public void registerBusiness(IBusiness<T> business) throws RegisterException {
        if (business == null) {
            throw new RegisterParamException("business should not be null");
        }

        for (Class<?> implExtClass : business.implementExtensionPoints()) {
            if (!allExtensionPointClasses.contains(implExtClass)) {
                throw new RegisterException(String.format("extension point [%s] not registered", implExtClass.getName()));
            }
        }

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

        if (enableLogger) {
            logger.info("{} register business: [{}]", LOG_PREFIX, business.code());
        }
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
        return extensionPointDefaultImplementation;
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
        if (extensionPointDefaultImplementation == null) {
            throw new SessionException("extension point default implementation not registered");
        }
        long startTime = System.currentTimeMillis();
        boolean defaultScope = EASY_EXTENSION_DEFAULT_SCOPE.equals(scope);

        if (defaultScope && session.hasScopedSession(EASY_EXTENSION_DEFAULT_SCOPE) && enableLogger) {
            logger.warn("{} session already initialized, this call will override previous session data", LOG_PREFIX);
        }
        // a failed init must not leave the previous (stale) session of this scope behind
        session.removeScopedSession(scope);
        scopedResolveTraces.get().remove(scope);

        if (enableLogger) {
            logger.info("{} {} init start", LOG_PREFIX, sessionLabel(scope));
        }

        Map<String, Integer> codePriorityMap;
        try {
            codePriorityMap = resolveMatchedCodeAndPriority(scope, param, startTime);
        } catch (SessionException e) {
            if (defaultScope) throw e;
            throw new SessionException(String.format("scope [%s], %s", scope, e.getMessage()), e);
        }
        try {
            for (Map.Entry<String, Integer> entry : codePriorityMap.entrySet()) {
                session.setScopedMatchedCode(scope, entry.getKey(), entry.getValue());
            }
        } catch (SessionException e) {
            // e.g. priority conflict with the default implementation: never keep a half-written session
            session.removeScopedSession(scope);
            scopedResolveTraces.get().remove(scope);
            throw e;
        }

        if (enableLogger) {
            logger.info("{} {} init completed, time cost: [{} ms]",
                    LOG_PREFIX, sessionLabel(scope), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Resolve matched codes and priorities, and build a structured trace.
     */
    private Map<String, Integer> resolveMatchedCodeAndPriority(String scope, T param, long startTime) throws SessionException {
        String scopePrefix = enableLogger
                ? (Objects.equals(scope, EASY_EXTENSION_DEFAULT_SCOPE)
                    ? "init session"
                    : "init session with scope: [%s],".formatted(scope))
                : "";

        Map<String, Integer> codePriorityMap = new HashMap<>();
        ResolveTrace.Builder traceBuilder = ResolveTrace.builder(scope);

        List<IBusiness<T>> matchedBusinesses = findMatchedBusinesses(param);
        enforceStrictBusinessMatching(matchedBusinesses);
        IBusiness<T> matchedBusiness = matchedBusinesses.isEmpty()
                ? null
                : businessMatchSelector.select(matchedBusinesses, param);

        if (matchedBusiness != null) {
            recordMatchedBusiness(matchedBusiness, codePriorityMap, traceBuilder, scopePrefix);
            resolveMatchedAbilities(matchedBusiness, param, codePriorityMap, traceBuilder, scopePrefix);
        }

        recordDefaultImplementation(codePriorityMap, traceBuilder, scopePrefix);

        traceBuilder.costMillis(System.currentTimeMillis() - startTime);
        ResolveTrace trace = traceBuilder.build();
        lastResolveTrace.set(trace);
        scopedResolveTraces.get().put(scope, trace);
        return codePriorityMap;
    }

    private List<IBusiness<T>> findMatchedBusinesses(T param) {
        List<IBusiness<T>> matched = new ArrayList<>();
        for (IBusiness<T> business : businessManager.listAllBusinesses()) {
            if (business.match(param)) {
                matched.add(business);
            }
        }
        return matched;
    }

    private void enforceStrictBusinessMatching(List<IBusiness<T>> matchedBusinesses) throws SessionException {
        if (!matchBusinessStrict) {
            return;
        }
        if (matchedBusinesses.isEmpty()) {
            throw new SessionException("no business matched");
        }
        if (matchedBusinesses.size() > 1) {
            List<String> codes = matchedBusinesses.stream().map(IBusiness::code).toList();
            throw new SessionException(String.format(
                    "multiple business found, matched business codes: [%s]", String.join(", ", codes)));
        }
    }

    private void recordMatchedBusiness(IBusiness<T> business, Map<String, Integer> codePriorityMap,
                                       ResolveTrace.Builder traceBuilder, String scopePrefix) {
        if (enableLogger) {
            logger.info("{} {} match business: [{}], priority: [{}]",
                    LOG_PREFIX, scopePrefix, business.code(), business.priority());
        }
        codePriorityMap.put(business.code(), business.priority());
        traceBuilder.matchedBusiness(business.code(), business.priority());
    }

    private void resolveMatchedAbilities(IBusiness<T> matchedBusiness, T param,
                                         Map<String, Integer> codePriorityMap,
                                         ResolveTrace.Builder traceBuilder,
                                         String scopePrefix) throws SessionException {
        List<UsedAbility> usedAbilities = matchedBusiness.usedAbilities();
        if (usedAbilities == null) {
            return;
        }
        for (UsedAbility usedAbility : usedAbilities) {
            IAbility<T> ability;
            try {
                ability = abilityManager.getAbility(usedAbility.code());
            } catch (QueryException e) {
                throw new SessionException(String.format(
                        "business [%s] used ability [%s] not found",
                        matchedBusiness.code(), usedAbility.code()));
            }
            if (ability.match(param)) {
                codePriorityMap.put(ability.code(), usedAbility.priority());
                if (enableLogger) {
                    logger.info("{} {} match ability: [{}], priority: [{}]",
                            LOG_PREFIX, scopePrefix, usedAbility.code(), usedAbility.priority());
                }
                traceBuilder.abilityMatched(ability.code(), usedAbility.priority());
            } else {
                traceBuilder.abilitySkipped(ability.code(), usedAbility.priority(),
                        "ability.match() returned false");
            }
        }
    }

    private void recordDefaultImplementation(Map<String, Integer> codePriorityMap,
                                             ResolveTrace.Builder traceBuilder,
                                             String scopePrefix) {
        codePriorityMap.put(extensionPointDefaultImplementation.code(), extensionPointDefaultImplementation.priority());
        traceBuilder.defaultImpl(extensionPointDefaultImplementation.code(), extensionPointDefaultImplementation.priority());
        if (enableLogger) {
            logger.info("{} {} match extension point default implementation: [{}], priority: [{}]",
                    LOG_PREFIX, scopePrefix,
                    extensionPointDefaultImplementation.code(),
                    extensionPointDefaultImplementation.priority());
        }
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
                    ? proxy.getInstance().getClass() : ability.getClass();
            Ability ann = abilityClass.getAnnotation(Ability.class);
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

    @Override
    public void removeSession() {
        session.removeAllSession();
        lastResolveTrace.remove();
        scopedResolveTraces.remove();
        if (enableLogger) {
            logger.info("{} all session (include scoped session) has been removed", LOG_PREFIX);
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

        ResolveTrace trace = scopedResolveTraces.get().get(scope);
        List<ResolveTrace.ResolutionEntry> chain = trace != null
                ? trace.getResolutionChain()
                : List.of();

        List<ExtensionExplanation.Candidate> candidates = new ArrayList<>(chain.size());
        ExtensionExplanation.Candidate selected = null;
        for (ResolveTrace.ResolutionEntry entry : chain) {
            Class<?> implClass = null;
            boolean implemented = false;
            try {
                E instance = extensionPointGroupImplementationManager
                        .findExtensionPointImplementationInstance(extensionPointType, entry.code());
                if (instance != null) {
                    implemented = true;
                    implClass = instance instanceof IProxy<?> p
                            ? p.getInstance().getClass()
                            : instance.getClass();
                }
            } catch (QueryException ignored) {
                // not implemented for this type; keep implemented=false
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
        if (scope == null) {
            throw new QueryNotFoundException("scope should not be null");
        }
        List<String> matchedCodes = getScopedAllMatchedCodes(scope);
        if (enableLogger) {
            logger.info("{} get first matched Extension<{}>{}, all candidate codes: [{}]",
                    LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), String.join(" > ", matchedCodes));
        }
        try {
            return getFirstMatchedExtensionByMatchedCodes(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            if (EASY_EXTENSION_DEFAULT_SCOPE.equals(scope)) throw e;
            throw new QueryException(String.format(
                    "get first matched Extension<%s> with scope: [%s] failed",
                    extensionType.getSimpleName(), scope), e);
        }
    }

    @Override
    public <E> List<E> getAllMatchedExtension(String scope, Class<E> extensionType) throws QueryException {
        if (scope == null) {
            throw new QueryNotFoundException("scope should not be null");
        }
        List<String> matchedCodes = getScopedAllMatchedCodes(scope);
        if (enableLogger) {
            logger.info("{} get all matched Extension<{}>{}, all candidate codes: [{}]",
                    LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), String.join(" > ", matchedCodes));
        }
        try {
            return getAllMatchedExtensionByMatchedCodes(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            if (EASY_EXTENSION_DEFAULT_SCOPE.equals(scope)) throw e;
            throw new QueryException(String.format(
                    "get all matched Extension<%s> with scope: [%s] failed",
                    extensionType.getSimpleName(), scope), e);
        }
    }

    private <E> List<E> getAllMatchedExtensionByMatchedCodes(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        List<E> extensions = new ArrayList<>();
        List<String> allMatchedCodes = new ArrayList<>();
        for (String code : matchedCodes) {
            E extension = extensionPointGroupImplementationManager.findExtensionPointImplementationInstance(extensionType, code);
            if (extension != null) {
                extensions.add(extension);
                allMatchedCodes.add(code);
            } else if (enableLogger) {
                logger.debug("{} get all matched Extension<{}>{}, instance with code: [{}] not matched, will be ignored", LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), code);
            }
        }

        if (enableLogger) {
            logger.info("{} get all matched Extension<{}>{}, hit instance with codes: [{}]", LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), String.join(" > ", allMatchedCodes));
        }
        return extensions;
    }

    private <E> E getFirstMatchedExtensionByMatchedCodes(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        for (String code : matchedCodes) {
            E extension = extensionPointGroupImplementationManager.findExtensionPointImplementationInstance(extensionType, code);
            if (extension != null) {
                if (enableLogger) {
                    logger.info("{} get first matched Extension<{}>{}, hit instance with code: [{}]", LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), code);
                }
                return extension;
            }
            if (enableLogger) {
                logger.debug("{} get first matched Extension<{}>{}, instance with code: [{}] not matched, will be ignored", LOG_PREFIX, extensionType.getSimpleName(), scopeSuffix(scope), code);
            }
        }
        throw new QueryNotFoundException(String.format("Extension<%s> not found", extensionType.getName()));
    }

    private List<String> getScopedAllMatchedCodes(String scope) throws QueryException {
        try {
            return session.getScopedMatchedCodes(scope);
        } catch (SessionException e) {
            throw new QueryNotFoundException(e.getMessage());
        }
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

    private static String scopeSuffix(String scope) {
        return EASY_EXTENSION_DEFAULT_SCOPE.equals(scope) ? "" : " with scope: [" + scope + "]";
    }

    private static String sessionLabel(String scope) {
        return EASY_EXTENSION_DEFAULT_SCOPE.equals(scope) ? "session" : "session with scope: [" + scope + "],";
    }

    private static String invokeErrorPrefix(String op, String scope) {
        return EASY_EXTENSION_DEFAULT_SCOPE.equals(scope)
                ? op + " failed, "
                : "scope " + scope + " " + op + " failed, ";
    }
}
