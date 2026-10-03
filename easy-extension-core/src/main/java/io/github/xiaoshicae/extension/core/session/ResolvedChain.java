package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The resolved identity of one request: which business, abilities and default implementations are active, in
 * priority order (lowest number first, i.e. highest priority first).
 * <p>
 * A chain is immutable and is computed once, when the session is initialized. Looking up an extension afterwards
 * only walks the chain. Because it is immutable it can also be handed to another thread (async hand-off, message
 * consumers, thread pools): capture it with {@code currentChain()} and bind it there with {@code bind(chain)},
 * without evaluating the matchers a second time.
 * </p>
 * <p>
 * {@link #registryVersion()} fingerprints the registry (extension points, abilities, businesses, default
 * implementations) the chain was resolved against. Binding refuses a chain resolved against a different registry,
 * because its codes may not mean the same thing there.
 * </p>
 *
 * @since 4.0
 */
public final class ResolvedChain {

    private final String registryVersion;
    private final List<ResolutionEntry> entries;
    private final List<String> codes;

    private ResolvedChain(String registryVersion, List<ResolutionEntry> entries, List<String> codes) {
        this.registryVersion = registryVersion;
        this.entries = entries;
        this.codes = codes;
    }

    private static final Comparator<ResolutionEntry> BY_PRIORITY = Comparator.comparingInt(ResolutionEntry::priority);

    /**
     * Up to this many entries, duplicate codes are found by comparing all pairs, which allocates nothing.
     */
    private static final int PAIRWISE_LIMIT = 16;

    /**
     * Create a chain. The entries are ordered by priority; the same code or the same priority must not appear twice.
     *
     * @param registryVersion fingerprint of the registry the entries were resolved against
     * @param entries         resolved entries, at least one, in any order
     * @throws IllegalArgumentException if {@code entries} is empty, an entry has no code, priority or kind, or a
     *                                  code or a priority already exists
     */
    public static ResolvedChain of(String registryVersion, Collection<? extends ResolutionEntry> entries) {
        Objects.requireNonNull(registryVersion, "registryVersion should not be null");
        Objects.requireNonNull(entries, "entries should not be null");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries should not be empty");
        }

        List<ResolutionEntry> sorted = new ArrayList<>(entries);
        for (ResolutionEntry entry : sorted) {
            if (entry == null || entry.code() == null || entry.priority() == null || entry.type() == null) {
                throw new IllegalArgumentException("every entry needs a code, a priority and a type");
            }
        }
        sorted.sort(BY_PRIORITY);

        List<String> codes = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            ResolutionEntry entry = sorted.get(i);
            if (i > 0 && entry.priority().equals(sorted.get(i - 1).priority())) {
                throw new IllegalArgumentException(String.format("priority [%d] already exist", entry.priority()));
            }
            codes.add(entry.code());
        }
        assertCodesAreUnique(codes);
        return new ResolvedChain(registryVersion, Collections.unmodifiableList(sorted), Collections.unmodifiableList(codes));
    }

    private static void assertCodesAreUnique(List<String> codes) {
        int size = codes.size();
        if (size <= PAIRWISE_LIMIT) {
            for (int i = 1; i < size; i++) {
                for (int j = 0; j < i; j++) {
                    if (codes.get(i).equals(codes.get(j))) {
                        throw new IllegalArgumentException(String.format("code [%s] already exist", codes.get(i)));
                    }
                }
            }
            return;
        }
        Set<String> seen = new HashSet<>(size * 2);
        for (String code : codes) {
            if (!seen.add(code)) {
                throw new IllegalArgumentException(String.format("code [%s] already exist", code));
            }
        }
    }

    /**
     * Fingerprint of the registry this chain was resolved against.
     */
    public String registryVersion() {
        return registryVersion;
    }

    /**
     * The entries (code, priority, kind) in priority order. Immutable.
     */
    public List<ResolutionEntry> entries() {
        return entries;
    }

    /**
     * The codes in priority order. Immutable, and the very same instance on every call: lookups on the hot path
     * use it without copying.
     */
    public List<String> codes() {
        return codes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResolvedChain other)) {
            return false;
        }
        return registryVersion.equals(other.registryVersion) && entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryVersion, entries);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResolvedChain{");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append(" > ");
            }
            sb.append(entries.get(i));
        }
        return sb.append(", registryVersion=").append(registryVersion).append('}').toString();
    }
}
