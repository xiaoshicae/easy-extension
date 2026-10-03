package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResolvedChainTest {

    private static ResolutionEntry business(String code, int priority) {
        return new ResolutionEntry(code, priority, EntryType.BUSINESS);
    }

    private static ResolutionEntry ability(String code, int priority) {
        return new ResolutionEntry(code, priority, EntryType.ABILITY);
    }

    private static ResolutionEntry defaults(String code, int priority) {
        return new ResolutionEntry(code, priority, EntryType.DEFAULT);
    }

    @Test
    public void testEntriesAreOrderedByPriority() {
        ResolvedChain chain = ResolvedChain.of("v1", List.of(defaults("d", Integer.MAX_VALUE), ability("a2", 7), business("b", 0), ability("a1", 3)));

        assertEquals(List.of("b", "a1", "a2", "d"), chain.codes());
        assertEquals(List.of(business("b", 0), ability("a1", 3), ability("a2", 7), defaults("d", Integer.MAX_VALUE)), chain.entries());
        assertEquals("v1", chain.registryVersion());
    }

    @Test
    public void testChainIsImmutableAndCodesAreNotCopiedPerCall() {
        List<ResolutionEntry> source = new java.util.ArrayList<>(List.of(business("b", 0), defaults("d", 10)));
        ResolvedChain chain = ResolvedChain.of("v1", source);

        source.add(ability("late", 5));
        assertEquals(List.of("b", "d"), chain.codes(), "later changes of the source list do not leak into the chain");

        assertSame(chain.codes(), chain.codes(), "hot path: the same list every time");
        assertThrows(UnsupportedOperationException.class, () -> chain.codes().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> chain.entries().add(business("x", 1)));
    }

    @Test
    public void testInvalidEntriesAreRejected() {
        IllegalArgumentException e;

        e = assertThrows(IllegalArgumentException.class, () -> ResolvedChain.of("v1", List.of()));
        assertEquals("entries should not be empty", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ResolvedChain.of("v1", List.of(business("b", 0), ability("b", 1))));
        assertEquals("code [b] already exist", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ResolvedChain.of("v1", List.of(business("b", 1), ability("a", 1))));
        assertEquals("priority [1] already exist", e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> ResolvedChain.of("v1", List.of(new ResolutionEntry("b", null, EntryType.BUSINESS))));
        assertEquals("every entry needs a code, a priority and a type", e.getMessage());

        NullPointerException npe = assertThrows(NullPointerException.class, () -> ResolvedChain.of(null, List.of(business("b", 0))));
        assertEquals("registryVersion should not be null", npe.getMessage());
    }

    @Test
    public void testLongChainsAreCheckedForDuplicatesToo() {
        java.util.List<ResolutionEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            entries.add(ability("a" + i, i));
        }
        assertEquals(40, ResolvedChain.of("v1", entries).codes().size());

        entries.add(ability("a7", 100));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ResolvedChain.of("v1", entries));
        assertEquals("code [a7] already exist", e.getMessage());
    }

    @Test
    public void testEqualityCoversEntriesAndRegistryVersion() {
        ResolvedChain a = ResolvedChain.of("v1", List.of(business("b", 0), defaults("d", 10)));
        ResolvedChain same = ResolvedChain.of("v1", List.of(defaults("d", 10), business("b", 0)));
        ResolvedChain otherVersion = ResolvedChain.of("v2", List.of(business("b", 0), defaults("d", 10)));
        ResolvedChain otherEntries = ResolvedChain.of("v1", List.of(business("b2", 0), defaults("d", 10)));

        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertNotEquals(a, otherVersion);
        assertNotEquals(a, otherEntries);
    }

    @Test
    public void testToStringReadsAsTheChain() {
        ResolvedChain chain = ResolvedChain.of("abc", List.of(business("b", 0), ability("a", 1), defaults("d", 10)));
        assertEquals("ResolvedChain{business(b, prio=0) > ability(a, prio=1) > default(d, prio=10), registryVersion=abc}", chain.toString());
    }
}
