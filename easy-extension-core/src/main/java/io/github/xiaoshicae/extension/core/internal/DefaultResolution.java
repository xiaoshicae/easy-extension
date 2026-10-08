package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.Resolution;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DefaultResolution<T> implements Resolution {
    private final Registry<T> registry;
    private final List<ChainItem> chain;
    private final ResolveTrace trace;

    DefaultResolution(Registry<T> registry, List<ChainItem> chain, ResolveTrace trace) {
        this.registry = registry;
        this.chain = chain;
        this.trace = trace;
    }

    boolean createdBy(Registry<?> registry) {
        return this.registry == registry;
    }

    @Override
    public String toString() {
        return "Resolution[business=" + trace.matchedBusinessCode() + ", chain=" + trace.chain()
                + ", skippedAbilities=" + trace.skippedAbilities().stream().map(ResolveTrace.SkippedAbility::code).toList() + "]";
    }

    @Override
    public <E> E first(Class<E> point) {
        Registry.DefaultEntry fallback = fallbackOf(point);
        for (ChainItem item : chain) {
            if (item.points().contains(point)) {
                return point.cast(item.impl());
            }
        }
        return point.cast(fallback.impl());
    }

    @Override
    public <E> List<E> all(Class<E> point) {
        Registry.DefaultEntry fallback = fallbackOf(point);
        List<E> result = new ArrayList<>();
        for (ChainItem item : chain) {
            if (item.points().contains(point)) {
                addDistinct(result, point.cast(item.impl()));
            }
        }
        addDistinct(result, point.cast(fallback.impl()));
        return Collections.unmodifiableList(result);
    }

    @Override
    public ResolveTrace trace() {
        return trace;
    }

    @Override
    public <E> ExtensionExplanation<E> explain(Class<E> point) {
        Registry.DefaultEntry fallback = fallbackOf(point);
        List<ExtensionExplanation.Candidate> candidates = new ArrayList<>();
        ExtensionExplanation.Candidate selected = null;
        for (ChainItem item : chain) {
            ExtensionExplanation.Candidate candidate = new ExtensionExplanation.Candidate(item.code(), item.position(),
                    item.type(), item.userClass(), item.points().contains(point));
            candidates.add(candidate);
            if (selected == null && candidate.implementsExtensionPoint()) {
                selected = candidate;
            }
        }
        ExtensionExplanation.Candidate fallbackCandidate = new ExtensionExplanation.Candidate(fallback.userClass().getName(),
                chain.size(), ResolveTrace.EntryType.DEFAULT, fallback.userClass(), true);
        candidates.add(fallbackCandidate);
        if (selected == null) {
            selected = fallbackCandidate;
        }
        return new ExtensionExplanation<>(point, candidates, selected);
    }

    // one object may play several roles (e.g. ability and default implementation): list it once
    private static <E> void addDistinct(List<E> list, E extension) {
        for (E existing : list) {
            if (existing == extension) {
                return;
            }
        }
        list.add(extension);
    }

    /**
     * The default implementation of a registered extension point; every registered point has one.
     */
    private Registry.DefaultEntry fallbackOf(Class<?> point) {
        Registry.DefaultEntry fallback = point == null ? null : registry.defaults().get(point);
        if (fallback == null) {
            throw new ResolutionException(Reason.EXTENSION_NOT_FOUND,
                    String.format("extension point [%s] is not registered", point == null ? null : point.getName()));
        }
        return fallback;
    }
}
