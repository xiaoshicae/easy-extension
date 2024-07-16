package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.DefaultExtGroupRealizationManager;
import io.github.xiaoshicae.extension.core.extension.IExtGroupRealization;
import io.github.xiaoshicae.extension.core.extension.IExtGroupRealizationManager;
import io.github.xiaoshicae.extension.core.session.DefaultSession;
import io.github.xiaoshicae.extension.core.session.ISession;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;
import io.github.xiaoshicae.extension.core.exception.ExtensionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DefaultExtContext<T> implements IExtContext<T> {
    private static final Logger logger = Logger.getLogger(DefaultExtContext.class.getName());

    private static final Integer defaultAbilityPriority = Integer.MAX_VALUE;

    private final Boolean matchBusinessStrict;

    private final Boolean enableLogger;

    private final ISession session = new DefaultSession() ;

    private final IAbilityManager<T> abilityManager = new DefaultAbilityManager<>();

    private final IBusinessManager<T> businessManager = new DefaultBusinessManager<>();

    private final IExtGroupRealizationManager extensionManager = new DefaultExtGroupRealizationManager();

    public DefaultExtContext() {
        this(false, false);
    }

    public DefaultExtContext(Boolean enableLogger, Boolean matchBusinessStrict) {
        this.enableLogger = enableLogger;
        this.matchBusinessStrict = matchBusinessStrict;
    }

    public void registerAbility(IAbility<T> ability) throws ExtensionException {
        if (enableLogger) {
            logger.info("[ExtensionFactory] register ability: " + ability.code());
        }

        abilityManager.registerAbility(ability);
        extensionManager.registerExtGroupRealization(ability, ability.code());
    }

    public void registerBusiness(IBusiness<T> business) throws ExtensionException {
        if (enableLogger) {
            logger.info("[ExtensionFactory] register business: " + business.code());
        }

        businessManager.registerBusiness(business);
        extensionManager.registerExtGroupRealization(business, business.code());
    }

    @Override
    public void initSession(T param) throws ExtensionException {
        // remove session before init
        session.remove();

        IBusiness<T> matchedBusiness = null;
        List<String> matchedBusinessCodes = new ArrayList<>();
        for (IBusiness<T> business : businessManager.listAllBusinesses()) {
            if (business.match(param)) {
                if (matchedBusiness == null) {
                    matchedBusiness = business; // first matched
                }
                matchedBusinessCodes.add(business.code());
            }
        }

        if (matchBusinessStrict) {
            if (matchedBusiness == null) {
                throw new ExtensionException("business not found");
            }
            if (matchedBusinessCodes.size() > 1) {
                throw new ExtensionException("multiple business found, matched business codes: [" + String.join(", ", matchedBusinessCodes) + "]");
            }
        }

        if (matchedBusiness != null) {
            if (enableLogger) {
                logger.info("[ExtensionFactory] init session match business: " + matchedBusiness.code() + " priority: " + matchedBusiness.priority());
            }

            session.setMatchedCode(matchedBusiness.code(), matchedBusiness.priority());
            for (UsedAbility usedAbility : matchedBusiness.usedAbilities()) {
                IAbility<T> ability = abilityManager.getAbility(usedAbility.code());
                if (ability == null) {
                    throw new ExtensionException("business " + matchedBusiness.code() + " used ability " + usedAbility.code() + " not found");
                }
                if (ability.match(param)) {
                    if (enableLogger) {
                        logger.info("[ExtensionFactory] init session match ability: " + usedAbility.code() + " priority: " + usedAbility.priority());
                    }
                    session.setMatchedCode(usedAbility.code(), usedAbility.priority());
                }
            }
        }

        IAbility<T> defaultAbility = abilityManager.getAbility(BaseDefaultAbility.DEFAULT_CODE);

        session.setMatchedCode(defaultAbility.code(), defaultAbilityPriority);
        if (enableLogger) {
            logger.info("[ExtensionFactory] init session match default ability: " + defaultAbility.code() + " priority: " + defaultAbilityPriority);
        }
    }

    @Override
    public void removeSession() {
        session.remove();

        if (enableLogger) {
            logger.info("[ExtensionFactory] session removed");
        }
    }

    @Override
    public <E> E getFirstMatchedExtension(Class<E> extensionType) throws ExtensionException {
        List<E> allMatchedExtension = getAllMatchedExtension(extensionType);
        if (allMatchedExtension.isEmpty()) {
            throw new ExtensionNotFoundException(extensionType.getName() + " not found");
        }
        E firstMatchedExtension = allMatchedExtension.get(0);
        if (enableLogger) {
            logger.info("[ExtensionFactory] get first matched extension instance: " + ((IExtGroupRealization<?>) firstMatchedExtension).code());
        }
        return firstMatchedExtension;
    }

    @Override
    public <E> List<E> getAllMatchedExtension(Class<E> extensionType) throws ExtensionException {
        List<String> matchedCodes;
        try {
            matchedCodes = session.getMatchedCodes();
        } catch (SessionException e) {
            throw new ExtensionNotFoundException(extensionType.getName() + " not found, getMatchedCodes failed", e);
        }

        if (enableLogger) {
            logger.info("[ExtensionFactory] get all matched candidate codes: " + String.join(" > ", matchedCodes));
        }

        List<E> extensions = new ArrayList<>();
        for (String code : matchedCodes) {
            E extension;
            try {
                extension = extensionManager.getExtGroupRealization(extensionType, code);
            } catch (ExtensionNotFoundException e) {
                continue;
            }
            if (extension != null) {
                extensions.add(extension);
            }
        }

        if (enableLogger) {
            logger.info("[ExtensionFactory] get all matched extension instance: " + extensions.stream().map(e -> ((IExtGroupRealization<?>) e).code()).collect(Collectors.joining(" > ")));
        }
        return extensions;
    }

    @Override
    public void validateContext() throws ExtensionException {
        List<Class<?>> allExtensionList = new ArrayList<>();

        for (IBusiness<T> business : businessManager.listAllBusinesses()) {
            if (business.usedAbilities() == null) {
                continue;
            }

            allExtensionList.addAll(business.implementsExtensions());

            Set<String> codeSet = new HashSet<>();
            Set<Integer> prioritySet = new HashSet<>();

            codeSet.add(business.code());
            prioritySet.add(business.priority());

            for (UsedAbility usedAbility : business.usedAbilities()) {
                IAbility<T> ability = abilityManager.getAbility(usedAbility.code());
                allExtensionList.addAll(ability.implementsExtensions());

                if (codeSet.contains(usedAbility.code())) {
                    throw new ExtensionException("business " + business.code() + " used ability code " + usedAbility.code() + " duplicate");
                }

                if (prioritySet.contains(usedAbility.priority())) {
                    throw new ExtensionException("business " + business.code() + " used ability priority " + usedAbility.priority() + " duplicate");
                }

                codeSet.add(usedAbility.code());
                prioritySet.add(usedAbility.priority());
            }
        }

        if (!allExtensionList.isEmpty()) {
            IAbility<T> defaultAbility = abilityManager.getAbility(BaseDefaultAbility.DEFAULT_CODE);
            List<String> notImplemented = new ArrayList<>();
            for (Class<?> clazz : allExtensionList) {
                if (!clazz.isAssignableFrom(defaultAbility.getClass())) {
                    notImplemented.add(clazz.getName());
                }
            }
            if (!notImplemented.isEmpty()) {
                throw new ExtensionException("default ability should implements all extension interface, but current default ability not implements extension " + Arrays.toString(notImplemented.stream().distinct().toArray()));
            }
        }
    }
}
