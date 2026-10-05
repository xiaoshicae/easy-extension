package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private Registry.BusinessEntry<T> selectBusiness(T param) {
        if (registry.businessResolver() != null) {
            Optional<String> code = registry.businessResolver().resolve(param);
            if (code == null || code.isEmpty()) {
                return noBusiness();
            }
            Registry.BusinessEntry<T> business = registry.businesses().get(code.get());
            if (business == null) {
                // an unknown business is "no business": an error in strict mode, default implementations otherwise
                if (registry.strict()) {
                    throw new ResolutionException(Reason.BUSINESS_NOT_FOUND,
                            String.format("business [%s] resolved by BusinessResolver is not registered", code.get()));
                }
                return null;
            }
            return business;
        }

        List<Registry.BusinessEntry<T>> matched = new ArrayList<>();
        for (Registry.BusinessEntry<T> business : registry.businesses().values()) {
            if (business.matcher().match(param)) {
                matched.add(business);
            }
        }
        if (matched.isEmpty()) {
            return noBusiness();
        }
        if (matched.size() == 1) {
            return matched.get(0);
        }
        List<String> codes = matched.stream().map(Registry.BusinessEntry::code).toList();
        if (registry.strict()) {
            throw new ResolutionException(Reason.MULTIPLE_BUSINESSES_MATCHED,
                    String.format("multiple business found, matched business codes: [%s]", String.join(", ", codes)));
        }
        String selected = registry.businessSelector().select(codes, param);
        if (selected == null) {
            return null;
        }
        return matched.stream().filter(business -> business.code().equals(selected)).findFirst()
                .orElseThrow(() -> new ResolutionException(Reason.BUSINESS_NOT_FOUND,
                        String.format("business [%s] chosen by BusinessSelector is not among the matched businesses [%s]",
                                selected, String.join(", ", codes))));
    }

    private Registry.BusinessEntry<T> noBusiness() {
        if (registry.strict()) {
            throw new ResolutionException(Reason.NO_BUSINESS_MATCHED, "no business matched");
        }
        return null;
    }
}
