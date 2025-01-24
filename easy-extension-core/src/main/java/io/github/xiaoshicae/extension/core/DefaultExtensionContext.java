package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultExtensionContext<T> implements IExtensionContext<T> {
    private static final Logger logger = Logger.getLogger(DefaultExtensionContext.class.getName());
    private static final String logPrefix = "[Easy Extension]";

    /**
     * Whether to match business strict.
     * <br>
     * <p>If strict, there is one and only one business should be matched.</p>
     * <p>If not strict, use extension default implementation when no business matched, use first matched business.</p>
     */
    private final Boolean matchBusinessStrict;

    /**
     * Whether logger enabled.
     */
    private final Boolean enableLogger;

    /**
     * Scoped session manager.
     */
    private final IScopedSessionManager session = new DefaultScopedSessionManager();

    /**
     * Default scope.
     */
    private final static String EASY_EXTENSION_DEFAULT_SCOPE = "__easy__extension__default__scope__";

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
    private final Set<Class<?>> allExtensionPointClasses = new LinkedHashSet<>();

    /**
     * Matcher param class.
     */
    private Class<T> matcherParamClass;

    /**
     * Extension point default implementation.
     */
    private IExtensionPointGroupDefaultImplementation<T> extensionPointDefaultImplementation;

    public DefaultExtensionContext() {
        this(false, false);
    }

    public DefaultExtensionContext(Boolean enableLogger, Boolean matchBusinessStrict) {
        this.enableLogger = enableLogger;
        this.matchBusinessStrict = matchBusinessStrict;
    }

    @Override
    public void registerExtensionPoint(Class<?> clazz) throws RegisterException {
        if (Objects.isNull(clazz)) {
            throw new RegisterParamException("clazz should not be null");
        }
        if (!clazz.isInterface()) {
            throw new RegisterParamException("clazz should be an interface type");
        }
        if (allExtensionPointClasses.contains(clazz)) {
            throw new RegisterDuplicateException(String.format("class [%s] already registered", clazz.getName()));
        }

        allExtensionPointClasses.add(clazz);

        if (enableLogger) {
            logger.info(String.format("%s register extension point class: [%s]", logPrefix, clazz.getSimpleName()));
        }
    }

    @Override
    public void registerMatcherParamClass(Class<T> matcherParamClass) throws RegisterException {
        if (Objects.isNull(matcherParamClass)) {
            throw new RegisterParamException("matcher param class should not be null");
        }

        if (!Objects.isNull(this.matcherParamClass)) {
            throw new RegisterDuplicateException("matcher param class already registered");
        }

        this.matcherParamClass = matcherParamClass;

        if (enableLogger) {
            logger.info(String.format("%s register matcher param class: [%s]", logPrefix, matcherParamClass.getSimpleName()));
        }
    }

    @Override
    public void registerExtensionPointDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException {
        if (Objects.isNull(instance)) {
            throw new RegisterParamException("extension point default implementation should not be null");
        }
        if (!Objects.isNull(extensionPointDefaultImplementation)) {
            throw new RegisterDuplicateException("extension point default implementation already registered");
        }

        List<Class<?>> implementsExtension = instance.implementExtensionPoints();
        List<Class<?>> notImplementClasses = new ArrayList<>();
        for (Class<?> extensionPointClass : allExtensionPointClasses) {
            if (!implementsExtension.contains(extensionPointClass)) {
                notImplementClasses.add(extensionPointClass);
            }
        }
        if (!notImplementClasses.isEmpty()) {
            throw new RegisterParamException(String.format("extension point default implementation should implement all extension point, but in fact, it has not implement [%s]", notImplementClasses.stream().map(Class::getName).collect(Collectors.joining(", "))));
        }

        extensionPointDefaultImplementation = instance;
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(instance);

        if (enableLogger) {
            String name;
            if (instance instanceof IProxy<?> proxy) {
                name = proxy.getInstance().getClass().getSimpleName();
            } else {
                name = instance.getClass().getSimpleName();
            }
            logger.info(String.format("%s register extension point default implementation: [%s]", logPrefix, name));
        }
    }

    @Override
    public void registerAbility(IAbility<T> ability) throws RegisterException {
        if (Objects.isNull(ability)) {
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
            logger.info(String.format("%s register ability: [%s]", logPrefix, ability.code()));
        }
    }

    @Override
    public void registerBusiness(IBusiness<T> business) throws RegisterException {
        if (Objects.isNull(business)) {
            throw new RegisterParamException("business should not be null");
        }

        for (Class<?> implExtClass : business.implementExtensionPoints()) {
            if (!allExtensionPointClasses.contains(implExtClass)) {
                throw new RegisterException(String.format("extension point [%s] not registered", implExtClass.getName()));
            }
        }

        Set<String> codeSet = new HashSet<>();
        Set<Integer> prioritySet = new HashSet<>();

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

        businessManager.registerBusiness(business);
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(business);

        if (enableLogger) {
            logger.info(String.format("%s register business: [%s]", logPrefix, business.code()));
        }
    }

    @Override
    public List<Class<?>> listAllExtensionPoint() {
        return allExtensionPointClasses.stream().toList();
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
        long startTime = System.currentTimeMillis();
        session.removeScopedSession(EASY_EXTENSION_DEFAULT_SCOPE);

        if (enableLogger) {
            logger.info(String.format("%s session init start", logPrefix));
        }

        Map<String, Integer> codePriorityMap = resolveMatchedCodeAndPriority(EASY_EXTENSION_DEFAULT_SCOPE, param);
        for (Map.Entry<String, Integer> entry : codePriorityMap.entrySet()) {
            session.setScopedMatchedCode(EASY_EXTENSION_DEFAULT_SCOPE, entry.getKey(), entry.getValue());
        }

        if (enableLogger) {
            logger.info(String.format("%s session init completed, time cost: [%d ms]", logPrefix, System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void initScopedSession(String scope, T param) throws SessionException {
        if (Objects.isNull(scope)) {
            throw new SessionParamException("scope should not be null");
        }

        long startTime = System.currentTimeMillis();
        session.removeScopedSession(scope);

        if (enableLogger) {
            logger.info(String.format("%s session with scope: [%s], init start", logPrefix, scope));
        }

        Map<String, Integer> codePriorityMap;
        try {
            codePriorityMap = resolveMatchedCodeAndPriority(scope, param);
        } catch (SessionException e) {
            throw new SessionException(String.format("scope [%s], ", e.getMessage()));
        }

        for (Map.Entry<String, Integer> entry : codePriorityMap.entrySet()) {
            session.setScopedMatchedCode(scope, entry.getKey(), entry.getValue());
        }
        if (enableLogger) {
            logger.info(String.format("%s session with scope: [%s], init completed, time cost: [%d ms]", logPrefix, scope, System.currentTimeMillis() - startTime));
        }
    }

    private Map<String, Integer> resolveMatchedCodeAndPriority(String scope, T param) throws SessionException {
        String scopePrefix = Objects.equals(scope, EASY_EXTENSION_DEFAULT_SCOPE) ? "init session" : "init session with scope: [%s],".formatted(scope);

        Map<String, Integer> codePriorityMap = new HashMap<>();

        IBusiness<T> matchedBusiness = null;
        List<String> matchedBusinessCodes = new ArrayList<>();
        for (IBusiness<T> business : businessManager.listAllBusinesses()) {
            if (business.match(param)) {
                if (Objects.isNull(matchedBusiness)) {
                    matchedBusiness = business;
                }
                matchedBusinessCodes.add(business.code());
            }
        }

        if (matchBusinessStrict) {
            if (Objects.isNull(matchedBusiness)) {
                throw new SessionException("no business matched");
            }
            if (matchedBusinessCodes.size() > 1) {
                throw new SessionException(String.format("multiple business found, matched business codes: [%s]", String.join(", ", matchedBusinessCodes)));
            }
        }

        if (!Objects.isNull(matchedBusiness)) {
            if (enableLogger) {
                logger.info(String.format("%s %s match business: [%s], priority: [%d]", logPrefix, scopePrefix, matchedBusiness.code(), matchedBusiness.priority()));
            }

            codePriorityMap.put(matchedBusiness.code(), matchedBusiness.priority());
            for (UsedAbility usedAbility : matchedBusiness.usedAbilities()) {
                IAbility<T> ability;
                try {
                    ability = abilityManager.getAbility(usedAbility.code());
                } catch (QueryException e) {
                    throw new SessionException(String.format("business [%s] used ability [%s] not found", matchedBusiness.code(), usedAbility.code()));
                }
                if (ability.match(param)) {
                    codePriorityMap.put(ability.code(), usedAbility.priority());
                    if (enableLogger) {
                        logger.info(String.format("%s %s match ability: [%s], priority: [%d]", logPrefix, scopePrefix, usedAbility.code(), usedAbility.priority()));
                    }
                }
            }
        }

        codePriorityMap.put(extensionPointDefaultImplementation.code(), extensionPointDefaultImplementation.priority());
        if (enableLogger) {
            logger.info(String.format("%s %s match extension point default implementation: [%s], priority: [%d]", logPrefix, scopePrefix, extensionPointDefaultImplementation.code(), extensionPointDefaultImplementation.priority()));
        }

        return codePriorityMap;
    }

    @Override
    public void removeSession() {
        session.removeAllSession();
        if (enableLogger) {
            logger.info(String.format("%s all session (include scoped session) has been removed", logPrefix));
        }
    }

    @Override
    public <E> E getFirstMatchedExtension(Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getScopedAllMatchedCodes(EASY_EXTENSION_DEFAULT_SCOPE);
        if (enableLogger) {
            logger.info(String.format("%s get first matched Extension<%s>, all candidate codes: [%s]", logPrefix, extensionType.getSimpleName(), String.join(" > ", matchedCodes)));
        }
        return getFirstMatchedExtensionByMatchedCodes(EASY_EXTENSION_DEFAULT_SCOPE, extensionType, matchedCodes);
    }

    @Override
    public <E> List<E> getAllMatchedExtension(Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getScopedAllMatchedCodes(EASY_EXTENSION_DEFAULT_SCOPE);
        if (enableLogger) {
            logger.info(String.format("%s get all matched Extension<%s>, all candidate codes: [%s]", logPrefix, extensionType.getSimpleName(), String.join(" > ", matchedCodes)));
        }
        return getAllMatchedExtensionByMatchedCodes(EASY_EXTENSION_DEFAULT_SCOPE, extensionType, matchedCodes);
    }

    @Override
    public <E> E getScopedFirstMatchedExtension(String scope, Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getScopedAllMatchedCodes(scope);
        if (enableLogger) {
            logger.info(String.format("%s get first matched Extension<%s> with scope: [%s], all candidate codes:[%s]", logPrefix, extensionType.getSimpleName(), scope, String.join(" > ", matchedCodes)));
        }

        E extension;
        try {
            extension = getFirstMatchedExtensionByMatchedCodes(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            throw new QueryException(String.format("get first matched Extension<%s> with scope: [%s] failed", extensionType.getSimpleName(), scope), e);
        }
        return extension;
    }

    public <E> List<E> getScopedAllMatchedExtension(String scope, Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getScopedAllMatchedCodes(scope);
        if (enableLogger) {
            logger.info(String.format("%s get all matched Extension<%s> with scope: [%s], all candidate candidate codes:[%s]", logPrefix, extensionType.getSimpleName(), scope, String.join(" > ", matchedCodes)));
        }

        List<E> extensions;
        try {
            extensions = getAllMatchedExtensionByMatchedCodes(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            throw new QueryException(String.format("get all matched Extension<%s> with scope: [%s] failed", extensionType.getSimpleName(), scope), e);
        }
        return extensions;
    }

    private <E> List<E> getAllMatchedExtensionByMatchedCodes(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        String scopePrefix = scope.equals(EASY_EXTENSION_DEFAULT_SCOPE) ? "" : " with scope: [%s]".formatted(scope);

        List<E> extensions = new ArrayList<>();
        List<String> allMatchedCodes = new ArrayList<>();
        for (String code : matchedCodes) {
            try {
                E extension = extensionPointGroupImplementationManager.getExtensionPointImplementationInstance(extensionType, code);
                if (!Objects.isNull(extension)) {
                    extensions.add(extension);
                    allMatchedCodes.add(code);
                }
            } catch (QueryNotFoundException ignored) {
                if (enableLogger) {
                    logger.fine(String.format("%s get all matched Extension<%s>%s, instance with code: [%s] not matched, will be ignored", logPrefix, extensionType.getSimpleName(), scopePrefix, code));
                }
            }
        }

        if (enableLogger) {
            logger.info(String.format("%s get all matched Extension<%s>%s, hit instance with codes: [%s]", logPrefix, extensionType.getSimpleName(), scopePrefix, String.join(" > ", allMatchedCodes)));
        }
        return extensions;
    }

    private <E> E getFirstMatchedExtensionByMatchedCodes(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        String scopePrefix = scope.equals(EASY_EXTENSION_DEFAULT_SCOPE) ? "" : " with scope: [%s]".formatted(scope);
        for (String code : matchedCodes) {
            try {
                E extension = extensionPointGroupImplementationManager.getExtensionPointImplementationInstance(extensionType, code);
                if (enableLogger) {
                    logger.info(String.format("%s get first matched Extension<%s>%s, hit instance with code: [%s]", logPrefix, extensionType.getSimpleName(), scopePrefix, code));
                }
                return extension;
            } catch (QueryNotFoundException e) {
                if (enableLogger) {
                    logger.fine(String.format("%s get first matched Extension<%s>%s, instance with code: [%s] not matched, will be ignored", logPrefix, extensionType.getSimpleName(), scopePrefix, code));
                }
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
        E extension;
        try {
            extension = getFirstMatchedExtension(extensionType);
        } catch (QueryException e) {
            throw new InvokeException("invoke failed, " + e.getMessage(), e);
        }
        return invoker.apply(extension);
    }

    @Override
    public <E, R> List<R> invokeAll(Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        List<E> extensions;
        try {
            extensions = getAllMatchedExtension(extensionType);
        } catch (QueryException e) {
            throw new InvokeException("invokeAll failed, " + e.getMessage(), e);
        }
        List<R> resList = new ArrayList<>();
        for (E extension : extensions) {
            R res = invoker.apply(extension);
            resList.add(res);
        }
        return resList;
    }

    @Override
    public <E, R> R scopedInvoke(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        E extension;
        try {
            extension = getScopedFirstMatchedExtension(scope, extensionType);
        } catch (QueryException e) {
            throw new InvokeException("scope %s scopedInvoke failed, %s".formatted(scope, e.getMessage()), e);
        }
        return invoker.apply(extension);
    }

    @Override
    public <E, R> List<R> scopedInvokeAll(String scope, Class<E> extensionType, Function<E, R> invoker) throws InvokeException {
        List<E> extensions;
        try {
            extensions = getScopedAllMatchedExtension(scope, extensionType);
        } catch (QueryException e) {
            throw new InvokeException("scope %s scopedInvokeAll failed, %s".formatted(scope, e.getMessage()), e);
        }
        List<R> resList = new ArrayList<>();
        for (E extension : extensions) {
            R res = invoker.apply(extension);
            resList.add(res);
        }
        return resList;
    }
}
