package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Identifier;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.extension.DefaultExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.session.DefaultSession;
import io.github.xiaoshicae.extension.core.session.ISession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultExtensionContext<T> implements IExtensionContext<T> {
    private static final Logger logger = Logger.getLogger(DefaultExtensionContext.class.getName());

    private static final String logPrefix = "Extension factory";

    private final Boolean matchBusinessStrict;

    private final Boolean enableLogger;

    private final ISession session = new DefaultSession();

    private final IAbilityManager<T> abilityManager = new DefaultAbilityManager<>();

    private final IBusinessManager<T> businessManager = new DefaultBusinessManager<>();

    private final IExtensionPointGroupImplementationManager<T> extensionPointGroupImplementationManager = new DefaultExtensionPointGroupImplementationManager<>();

    private final Set<Class<?>> allExtensionPointClasses = new LinkedHashSet<>();

    private Class<T> matcherParamClass;

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
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(instance, resolveDefaultImplementation(instance));
        if (enableLogger) {
            logger.info(String.format("%s register extension point default implementation: [%s]", logPrefix, instance.getClass().getSimpleName()));
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
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(ability, resolveAbilityName(ability));
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
        extensionPointGroupImplementationManager.registerExtensionPointImplementationInstance(business, resolveBusinessName(business));
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
        if (enableLogger) {
            logger.info(String.format("%s session init", logPrefix));
        }

        session.remove();

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
                logger.info(String.format("%s init session match business:[%s], priority:[%d]", logPrefix, matchedBusiness.code(), matchedBusiness.priority()));
            }

            session.setMatchedCode(resolveBusinessName(matchedBusiness), matchedBusiness.priority());
            for (UsedAbility usedAbility : matchedBusiness.usedAbilities()) {
                IAbility<T> ability;
                try {
                    ability = abilityManager.getAbility(usedAbility.code());
                } catch (QueryException e) {
                    throw new SessionException(String.format("business [%s] used ability [%s] not found", matchedBusiness.code(), usedAbility.code()));
                }
                if (ability.match(param)) {
                    session.setMatchedCode(resolveAbilityName(ability), usedAbility.priority());
                    if (enableLogger) {
                        logger.info(String.format("%s init session match ability:[%s], priority:[%d]", logPrefix, usedAbility.code(), usedAbility.priority()));
                    }
                }
            }
        }

        session.setMatchedCode(resolveDefaultImplementation(extensionPointDefaultImplementation), extensionPointDefaultImplementation.priority());
        if (enableLogger) {
            logger.info(String.format("%s init session match extension point default implementation:[%s], priority:[%d]", logPrefix, extensionPointDefaultImplementation.code(), extensionPointDefaultImplementation.priority()));
        }
    }

    @Override
    public void removeSession() {
        session.remove();
        if (enableLogger) {
            logger.info(String.format("%s session removed", logPrefix));
        }
    }

    @Override
    public <E> E getFirstMatchedExtension(Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getAllMatchedCodes();
        if (enableLogger) {
            logger.info(String.format("%s Extension<%s> get first matched instance, all matched candidate codes:[%s]", logPrefix, extensionType.getSimpleName(), String.join(" > ", matchedCodes)));
        }

        for (String code : matchedCodes) {
            try {
                E extension = extensionPointGroupImplementationManager.getExtensionPointImplementationInstance(extensionType, code);
                if (enableLogger) {
                    logger.info(String.format("%s Extension<%s> get first matched instance:[%s]", logPrefix, extensionType.getSimpleName(), code));
                }
                return extension;
            } catch (QueryNotFoundException e) {
                if (enableLogger) {
                    logger.fine(String.format("%s Extension<%s> get first matched instance, code:[%s] not matched", logPrefix, extensionType.getSimpleName(), code));
                }
            }
        }

        throw new QueryNotFoundException(String.format("Extension<%s> not found", extensionType.getName()));
    }

    @Override
    public <E> List<E> getAllMatchedExtension(Class<E> extensionType) throws QueryException {
        List<String> matchedCodes = getAllMatchedCodes();

        if (enableLogger) {
            logger.info(String.format("%s Extension<%s> get all matched instance, all matched candidate candidate codes:[%s]", logPrefix, extensionType.getSimpleName(), String.join(" > ", matchedCodes)));
        }

        List<E> extensions = new ArrayList<>();
        for (String code : matchedCodes) {
            try {
                E extension = extensionPointGroupImplementationManager.getExtensionPointImplementationInstance(extensionType, code);
                if (!Objects.isNull(extension)) {
                    extensions.add(extension);
                }
            } catch (QueryNotFoundException ignored) {
                if (enableLogger) {
                    logger.fine(String.format("%s Extension<%s> get all matched instance, code:[%s] not matched", logPrefix, extensionType.getSimpleName(), code));
                }
            }
        }

        if (enableLogger) {
            String allMatchedCodes = extensions.stream().map(e -> ((Identifier) e).code()).collect(Collectors.joining(" > "));
            logger.info(String.format("%s Extension<%s> get all matched instance:[%s]", logPrefix, extensionType.getSimpleName(), allMatchedCodes));
        }
        return extensions;
    }

    private List<String> getAllMatchedCodes() throws QueryException {
        try {
            return session.getMatchedCodes();
        } catch (SessionException e) {
            throw new QueryNotFoundException(e.getMessage());
        }
    }

    private String resolveDefaultImplementation(IExtensionPointGroupDefaultImplementation<T> instance) {
        return "default-implementation@" + instance.code();
    }

    private String resolveAbilityName(IAbility<T> ability) {
        return "ability@" + ability.code();
    }

    private String resolveBusinessName(IBusiness<T> ability) {
        return "business@" + ability.code();
    }
}
