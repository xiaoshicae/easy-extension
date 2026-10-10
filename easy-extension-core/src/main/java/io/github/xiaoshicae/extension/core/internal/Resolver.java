package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a request parameter into a {@link DefaultResolution}: pick the business, then evaluate the abilities it mounts.
 */
final class Resolver<T> {
    private static final Logger logger = LoggerFactory.getLogger(Resolver.class);

    private final Registry<T> registry;

    Resolver(Registry<T> registry) {
        this.registry = registry;
    }

    DefaultResolution<T> resolve(T param) {
        long start = System.nanoTime();
        Registry.BusinessEntry<T> business = selectBusiness(param);

        List<ChainItem> chain = new ArrayList<>();
        List<ResolveTrace.SkippedAbility> skipped = new ArrayList<>();
        if (business != null) {
            int position = 0;
            for (MountInfo mount : business.mounts()) {
                if (mount.isSelf()) {
                    chain.add(new ChainItem(business.code(), ResolveTrace.EntryType.BUSINESS, position,
                            business.impl(), business.userClass(), business.points()));
                } else {
                    Registry.AbilityEntry<T> ability = registry.abilities().get(mount.abilityCode());
                    if (ability.matcher().match(param)) {
                        chain.add(new ChainItem(ability.code(), ResolveTrace.EntryType.ABILITY, position,
                                ability.impl(), ability.userClass(), ability.points()));
                    } else {
                        skipped.add(new ResolveTrace.SkippedAbility(ability.code(), position, "ability.match() returned false"));
                    }
                }
                position++;
            }
        }

        List<ResolveTrace.ChainEntry> entries = chain.stream()
                .map(item -> new ResolveTrace.ChainEntry(item.code(), item.type(), item.position()))
                .toList();
        ResolveTrace trace = new ResolveTrace(business == null ? null : business.code(), entries, skipped,
                System.nanoTime() - start);
        if (logger.isDebugEnabled()) {
            logger.debug("[Easy Extension] resolved business [{}], chain {}, skipped abilities {}, cost {} ms",
                    trace.matchedBusinessCode(), trace.chain(), trace.skippedAbilities(), trace.costMillis());
        }
        return new DefaultResolution<>(registry, List.copyOf(chain), trace);
    }

    /**
     * One pass, every matcher asked once: several matches is an error in strict and non-strict mode alike. The list
     * of codes is only built once a second business matches.
     */
    private Registry.BusinessEntry<T> selectBusiness(T param) {
        Registry.BusinessEntry<T> selected = null;
        List<String> codes = null;
        for (Registry.BusinessEntry<T> business : registry.businesses().values()) {
            if (!business.matcher().match(param)) {
                continue;
            }
            if (selected == null) {
                selected = business;
                continue;
            }
            if (codes == null) {
                codes = new ArrayList<>();
                codes.add(selected.code());
            }
            codes.add(business.code());
        }
        if (codes != null) {
            throw new ResolutionException(Reason.MULTIPLE_BUSINESSES_MATCHED,
                    String.format("multiple business found, matched business codes: [%s]", String.join(", ", codes)));
        }
        return selected != null ? selected : noBusiness();
    }

    private Registry.BusinessEntry<T> noBusiness() {
        if (registry.strict()) {
            throw new ResolutionException(Reason.NO_BUSINESS_MATCHED, "no business matched");
        }
        return null;
    }
}
