package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.ability.IAbilityManager;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.IBusinessManager;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.OrderedCodeBusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Decides who a request is: which business matches, which of its abilities are active, and which default
 * implementations stand behind them, as a {@link ResolvedChain} in priority order.
 * <p>
 * Pure with respect to the session: it reads the registry and the request's param and returns the outcome;
 * binding the chain to a session is the caller's business.
 * </p>
 *
 * @param <T> matcher param class
 */
final class ChainResolver<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExtensionContext.class);
    private static final String LOG_PREFIX = DefaultExtensionContext.LOG_PREFIX;

    /**
     * Upper bound for the combinations of multiply-matching businesses that are warned about, so that a
     * pathological param space cannot grow the set without limit.
     */
    private static final int MAX_WARNED_COMBINATIONS = 128;

    /**
     * Outcome of one resolution: the chain to bind, and the trace that explains it.
     *
     * @param chain the resolved chain
     * @param trace why the chain looks the way it does
     */
    record Resolution(ResolvedChain chain, ResolveTrace trace) {
    }

    private final IBusinessManager<T> businessManager;
    private final IAbilityManager<T> abilityManager;
    private final DefaultsRegistry<T> defaults;
    private final UnknownBusinessPolicy unknownBusinessPolicy;
    private final MultiMatchPolicy multiMatchPolicy;
    private final Supplier<BusinessMatchSelector<T>> selector;
    private final boolean enableLogger;
    private final Set<Integer> warnedCombinations = ConcurrentHashMap.newKeySet();

    ChainResolver(IBusinessManager<T> businessManager, IAbilityManager<T> abilityManager, DefaultsRegistry<T> defaults,
                  UnknownBusinessPolicy unknownBusinessPolicy, MultiMatchPolicy multiMatchPolicy,
                  Supplier<BusinessMatchSelector<T>> selector, boolean enableLogger) {
        this.businessManager = businessManager;
        this.abilityManager = abilityManager;
        this.defaults = defaults;
        this.unknownBusinessPolicy = unknownBusinessPolicy;
        this.multiMatchPolicy = multiMatchPolicy;
        this.selector = selector;
        this.enableLogger = enableLogger;
    }

    /**
     * How many distinct combinations of multiply-matching businesses have been warned about so far.
     *
     * @return the number of combinations
     */
    int multiMatchWarningCount() {
        return warnedCombinations.size();
    }

    /**
     * Resolve who the request is.
     *
     * @param scope           scope the result is meant for, for messages and the trace
     * @param defaultScope    whether that is the default scope
     * @param param           the request, as far as matching is concerned
     * @param registryVersion fingerprint of the registry, recorded in the chain
     * @return the chain and its trace
     * @throws SessionException if the policies reject the request or the registry is inconsistent
     */
    Resolution resolve(String scope, boolean defaultScope, T param, String registryVersion) throws SessionException {
        long startTime = System.currentTimeMillis();
        String logPrefix = enableLogger
                ? (defaultScope ? "init session" : "init session with scope: [%s],".formatted(scope))
                : null;

        ResolveTrace.Builder trace = ResolveTrace.builder(scope);
        // by code: if a business and an ability share a code the later one replaces the earlier, as it always did
        Map<String, ResolutionEntry> entries = new LinkedHashMap<>();

        IBusiness<T> business = chooseBusiness(findMatchedBusinesses(param), param);
        if (business != null) {
            recordBusiness(business, entries, trace, logPrefix);
            resolveAbilities(business, param, entries, trace, logPrefix);
        }
        recordDefaults(entries, trace, logPrefix);

        if (entries.isEmpty()) {
            // nothing to resolve to: no business, and no default implementation to fall back on
            throw new SessionException("no business matched");
        }
        ResolvedChain chain = buildChain(scope, defaultScope, entries.values(), registryVersion);
        trace.costMillis(System.currentTimeMillis() - startTime);
        return new Resolution(chain, trace.build());
    }

    private List<IBusiness<T>> findMatchedBusinesses(T param) {
        List<IBusiness<T>> all = businessManager.listAllBusinesses();
        List<IBusiness<T>> matched = null;
        for (int i = 0; i < all.size(); i++) {
            IBusiness<T> business = all.get(i);
            if (business.match(param)) {
                if (matched == null) {
                    matched = new ArrayList<>(2);
                }
                matched.add(business);
            }
        }
        return matched == null ? List.of() : matched;
    }

    private IBusiness<T> chooseBusiness(List<IBusiness<T>> matched, T param) throws SessionException {
        if (matched.isEmpty()) {
            if (unknownBusinessPolicy == UnknownBusinessPolicy.REJECT) {
                throw new SessionException("no business matched");
            }
            return null;
        }
        if (matched.size() > 1 && multiMatchPolicy == MultiMatchPolicy.REJECT) {
            List<String> codes = matched.stream().map(IBusiness::code).toList();
            throw new SessionException(String.format(
                    "multiple business found, matched business codes: [%s]", String.join(", ", codes)));
        }
        BusinessMatchSelector<T> chosenBy = selector.get();
        IBusiness<T> selected = chosenBy.select(matched, param);
        if (matched.size() > 1 && leavesItToRegistrationOrder(chosenBy)) {
            warnAboutMultiMatch(matched, selected);
        }
        return selected;
    }

    /**
     * Whether the pick between several matching businesses is left to the order they were registered in: the
     * default selector with no order configured. A configured order and a selector of the application's own are its
     * rule for such a case, which is nothing to warn about.
     */
    private static boolean leavesItToRegistrationOrder(BusinessMatchSelector<?> selector) {
        return selector instanceof OrderedCodeBusinessMatchSelector<?> ordered && ordered.order().isEmpty();
    }

    /**
     * Several businesses matched and nothing the application configured decided between them: the first registered
     * one wins. That is usually a mistake in the matchers and picking silently would hide it, so say so: once per
     * distinct combination, not once per request.
     *
     * @param matched  the businesses that matched
     * @param selected the one the selector picked, if any
     */
    private void warnAboutMultiMatch(List<IBusiness<T>> matched, IBusiness<T> selected) {
        int combination = 1;
        for (IBusiness<T> business : matched) {
            combination = 31 * combination + business.code().hashCode();
        }
        if (warnedCombinations.contains(combination) || warnedCombinations.size() >= MAX_WARNED_COMBINATIONS) {
            return;
        }
        if (warnedCombinations.add(combination)) {
            String codes = String.join(", ", matched.stream().map(IBusiness::code).toList());
            logger.warn("{} multiple businesses matched [{}], selected [{}] by registration order. Overlapping business "
                            + "matchers are usually a configuration mistake: fix the matchers, say which one should win "
                            + "(easy-extension.business-match-order or a BusinessMatchSelector), or set "
                            + "easy-extension.multi-match-policy=reject to fail fast. (logged once per combination)",
                    LOG_PREFIX, codes, selected == null ? "none" : selected.code());
        }
    }

    private void recordBusiness(IBusiness<T> business, Map<String, ResolutionEntry> entries,
                                ResolveTrace.Builder trace, String logPrefix) {
        if (enableLogger) {
            logger.info("{} {} match business: [{}], priority: [{}]",
                    LOG_PREFIX, logPrefix, business.code(), business.priority());
        }
        entries.put(business.code(), new ResolutionEntry(business.code(), business.priority(), EntryType.BUSINESS));
        trace.matchedBusiness(business.code(), business.priority());
    }

    private void resolveAbilities(IBusiness<T> business, T param, Map<String, ResolutionEntry> entries,
                                  ResolveTrace.Builder trace, String logPrefix) throws SessionException {
        List<UsedAbility> usedAbilities = business.usedAbilities();
        if (usedAbilities == null) {
            return;
        }
        for (UsedAbility usedAbility : usedAbilities) {
            IAbility<T> ability;
            try {
                ability = abilityManager.findAbility(usedAbility.code());
            } catch (QueryException e) {
                ability = null;
            }
            if (ability == null) {
                throw new SessionException(String.format(
                        "business [%s] used ability [%s] not found", business.code(), usedAbility.code()));
            }
            if (ability.match(param)) {
                entries.put(ability.code(), new ResolutionEntry(ability.code(), usedAbility.priority(), EntryType.ABILITY));
                if (enableLogger) {
                    logger.info("{} {} match ability: [{}], priority: [{}]",
                            LOG_PREFIX, logPrefix, usedAbility.code(), usedAbility.priority());
                }
                trace.abilityMatched(ability.code(), usedAbility.priority());
            } else {
                trace.abilitySkipped(ability.code(), usedAbility.priority(), "ability.match() returned false");
            }
        }
    }

    private void recordDefaults(Map<String, ResolutionEntry> entries, ResolveTrace.Builder trace, String logPrefix) {
        List<ResolutionEntry> defaultEntries = defaults.entries();
        for (int i = 0; i < defaultEntries.size(); i++) {
            ResolutionEntry entry = defaultEntries.get(i);
            entries.put(entry.code(), entry);
            trace.defaultImpl(entry.code(), entry.priority());
            if (enableLogger) {
                logger.info("{} {} match extension point default implementation: [{}], priority: [{}]",
                        LOG_PREFIX, logPrefix, entry.code(), entry.priority());
            }
        }
    }

    private ResolvedChain buildChain(String scope, boolean defaultScope, Collection<ResolutionEntry> entries, String registryVersion) throws SessionException {
        for (ResolutionEntry entry : entries) {
            if (entry.priority() == null) {
                throw new SessionParamException(String.format("%spriority of %s should not be null",
                        scopePrefix(scope, defaultScope), describe(entry)));
            }
        }
        try {
            return ResolvedChain.of(registryVersion, entries);
        } catch (IllegalArgumentException e) {
            // two entries with the same priority (codes were made unique by the map the entries were collected in)
            throw new SessionParamException(scopePrefix(scope, defaultScope) + clashBetween(entries, e));
        }
    }

    /**
     * Say who shares the priority. Only on the failure path, so it may look at every pair.
     */
    private static String clashBetween(Collection<ResolutionEntry> entries, IllegalArgumentException cause) {
        List<ResolutionEntry> all = new ArrayList<>(entries);
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (all.get(i).priority().equals(all.get(j).priority())) {
                    return String.format("priority [%d] is taken by both %s and %s",
                            all.get(i).priority(), describe(all.get(i)), describe(all.get(j)));
                }
            }
        }
        return cause.getMessage();
    }

    private static String describe(ResolutionEntry entry) {
        return (entry.type() == EntryType.DEFAULT ? "default implementation" : entry.type().label()) + " [" + entry.code() + "]";
    }

    /**
     * A message about a named scope says which; the default scope is not something the application named.
     */
    private static String scopePrefix(String scope, boolean defaultScope) {
        return defaultScope ? "" : "scope [" + scope + "], ";
    }
}
