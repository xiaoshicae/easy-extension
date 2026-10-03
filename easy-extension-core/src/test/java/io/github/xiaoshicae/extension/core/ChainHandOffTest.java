package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopAbility;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.session.ExtensionSessionScope;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resolved identity of a request can be captured on one thread and bound on another, without matching again.
 */
public class ChainHandOffTest {

    private static final String DEFAULT_CODE = "system.extension.point.default.implementation";
    private static final String NO_SESSION = "invoke failed, scope [__easy__extension__default__scope__], matched codes is empty, may be session not init";

    private final AtomicInteger matcherCalls = new AtomicInteger();

    /** "retail" mounts the ability "promo", "wholesale" mounts nothing. Counts every business match. */
    private DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerAbility(new ShopAbility("promo", r -> true, Pricing.class));
        context.registerBusiness(new ShopBusiness("retail", r -> {
            matcherCalls.incrementAndGet();
            return "retail".equals(r.name);
        }, 0, List.of(Pricing.class, Tax.class), List.of(new UsedAbility("promo", 1))));
        context.registerBusiness(ShopBusiness.named("wholesale", Pricing.class, Tax.class));
        return context;
    }

    @Test
    public void testResolveBindsNothing() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        ResolvedChain chain = context.resolve(new Req("retail"));

        assertEquals(List.of("retail", "promo", DEFAULT_CODE), chain.codes());
        assertNull(context.currentChain());
        assertNull(context.getLastResolveTrace());
        InvokeException e = assertThrows(InvokeException.class, () -> context.invoke(Pricing.class, Pricing::price));
        assertEquals(NO_SESSION, e.getMessage());
    }

    @Test
    public void testCurrentChainIsWhatInitSessionResolved() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        context.initSession(new Req("retail"));
        try {
            assertEquals(context.resolve(new Req("retail")), context.currentChain());
            assertEquals(context.registryVersion(), context.currentChain().registryVersion());
        } finally {
            context.removeSession();
        }
        assertNull(context.currentChain());
    }

    @Test
    public void testChainServesAnotherThreadWithoutMatchingAgain() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain chain = context.resolve(new Req("retail"));
        int matchesSoFar = matcherCalls.get();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            String result = pool.submit(() -> ExtensionSessionScope.runWith(context, chain,
                    () -> context.invoke(Pricing.class, Pricing::price))).get();

            assertEquals("promo pricing", result.replace("retail", "promo"), "served by the chain's first implementer of Pricing");
            assertEquals("retail pricing", result);
            assertEquals(matchesSoFar, matcherCalls.get(), "binding evaluates no matcher");

            // the worker is clean afterwards: nothing leaks into whatever task the pool runs next
            Future<String> next = pool.submit(() -> context.invoke(Pricing.class, Pricing::price));
            ExecutionException e = assertThrows(ExecutionException.class, next::get);
            assertInstanceOf(InvokeException.class, e.getCause());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testRestoreOpensAndClosesLikeOpen() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain chain = context.resolve(new Req("wholesale"));

        try (var scope = ExtensionSessionScope.restore(context, chain)) {
            assertNotNull(scope);
            assertEquals("wholesale pricing", context.invoke(Pricing.class, Pricing::price));
            assertEquals(chain, context.currentChain());
        }

        assertNull(context.currentChain());
        InvokeException e = assertThrows(InvokeException.class, () -> context.invoke(Pricing.class, Pricing::price));
        assertEquals(NO_SESSION, e.getMessage());
    }

    @Test
    public void testRestoreOnAThreadThatHasASessionPutsItBack() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            ResolvedChain mine = context.currentChain();

            try (var scope = ExtensionSessionScope.restore(context, wholesale)) {
                assertNotNull(scope);
                assertEquals("wholesale pricing", context.invoke(Pricing.class, Pricing::price));
            }

            assertEquals(mine, context.currentChain(), "the session the thread had is bound again");
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRunWithOnTheThreadThatHandedTheTaskOverKeepsItsSession() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            ResolvedChain mine = context.currentChain();

            // what a direct executor, CallerRunsPolicy on a saturated pool or a parallel stream does:
            // the task runs on the thread that has a session of its own
            String inside = ExtensionSessionScope.runWith(context, wholesale, () -> context.invoke(Pricing.class, Pricing::price));
            List<String> events = new ArrayList<>();
            ExtensionSessionScope.runWith(context, wholesale, () -> {
                events.add(context.invoke(Pricing.class, Pricing::price));
            });

            assertEquals("wholesale pricing", inside);
            assertEquals(List.of("wholesale pricing"), events);
            assertEquals(mine, context.currentChain(), "the session of the thread is back");
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRunWithPutsTheSessionBackWhenTheBodyFails() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            ResolvedChain mine = context.currentChain();

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> ExtensionSessionScope.runWith(context, wholesale, () -> {
                throw new IllegalStateException("boom");
            }));

            assertEquals("boom", e.getMessage());
            assertEquals(mine, context.currentChain());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRunWithOnAThreadWithoutSessionLeavesNothingBehind() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        // a scope of its own that nobody cleaned up, as a task that forgot to would leave on a pooled thread
        context.bind("leftover", wholesale);
        try {
            ExtensionSessionScope.runWith(context, wholesale, () -> {
                assertEquals("wholesale pricing", context.invoke(Pricing.class, Pricing::price));
            });

            assertNull(context.currentChain());
            assertNull(context.currentChain("leftover"), "no session before, so no session after: whatever else was bound is gone too");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRestoreScopedPutsThePreviousSessionOfThatScopeBack() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain retail = context.resolve(new Req("retail"));
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.bind("tenant-x", retail);
        try {
            try (var scope = ExtensionSessionScope.restoreScoped(context, "tenant-x", wholesale)) {
                assertNotNull(scope);
                assertEquals("wholesale pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
            }

            assertEquals(retail, context.currentChain("tenant-x"));
            assertEquals("retail pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testPreviousSessionIsRemovedWhenItCannotBeBoundAnymore() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            try (var scope = ExtensionSessionScope.restore(context, wholesale)) {
                assertNotNull(scope);
                // the registry moved on while the task ran, the chain the thread had no longer fits it
                context.registerBusiness(ShopBusiness.named("distributor", Pricing.class));
            }

            assertNull(context.currentChain(), "removed rather than bound against a registry it was not resolved against");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testBindRefusesAChainOfAnotherRegistry() throws Exception {
        DefaultExtensionContext<Req> first = shop();
        DefaultExtensionContext<Req> second = shop();
        second.registerBusiness(ShopBusiness.named("distributor", Pricing.class));
        ResolvedChain chain = first.resolve(new Req("retail"));

        assertNotEquals(first.registryVersion(), second.registryVersion());
        SessionException e = assertThrows(SessionException.class, () -> second.bind(chain));
        assertEquals("chain was resolved against a different registry (chain version [" + first.registryVersion()
                + "], current version [" + second.registryVersion() + "]), resolve it again", e.getMessage());
        assertNull(second.currentChain(), "nothing was bound");
    }

    @Test
    public void testBindRefusesCodesTheRegistryDoesNotKnow() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        String version = context.registryVersion();
        ResolutionEntry defaults = new ResolutionEntry(DEFAULT_CODE, Integer.MAX_VALUE, EntryType.DEFAULT);

        SessionException e = assertThrows(SessionException.class, () -> context.bind(
                ResolvedChain.of(version, List.of(new ResolutionEntry("ghost", 0, EntryType.BUSINESS), defaults))));
        assertEquals("chain refers to unknown business [ghost]", e.getMessage());

        e = assertThrows(SessionException.class, () -> context.bind(
                ResolvedChain.of(version, List.of(new ResolutionEntry("ghost", 1, EntryType.ABILITY), defaults))));
        assertEquals("chain refers to unknown ability [ghost]", e.getMessage());

        e = assertThrows(SessionException.class, () -> context.bind(
                ResolvedChain.of(version, List.of(new ResolutionEntry("ghost", 1, EntryType.DEFAULT)))));
        assertEquals("chain refers to unknown default [ghost]", e.getMessage());
    }

    @Test
    public void testBindChecksItsArguments() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        SessionException e = assertThrows(SessionException.class, () -> context.bind(null));
        assertEquals("chain should not be null", e.getMessage());

        ResolvedChain chain = context.resolve(new Req("retail"));
        e = assertThrows(SessionException.class, () -> context.bind(null, chain));
        assertEquals("scope should not be null", e.getMessage());
    }

    @Test
    public void testBoundSessionIsExplainedAndTraced() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.bind(context.resolve(new Req("retail")));
        try {
            assertEquals("retail", context.explain(Pricing.class).selected().code());
            assertEquals(List.of("retail", "promo", DEFAULT_CODE),
                    context.explain(Pricing.class).candidates().stream().map(c -> c.code()).toList());

            assertEquals("retail", context.getLastResolveTrace().getMatchedBusinessCode());
            assertEquals(DEFAULT_CODE, context.getLastResolveTrace().getDefaultImplCode());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testScopesAreBoundAndRemovedOneByOne() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain retail = context.resolve(new Req("retail"));
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));

        context.bind(retail);
        context.bind("tenant-x", wholesale);
        try {
            assertEquals(retail, context.currentChain());
            assertEquals(wholesale, context.currentChain("tenant-x"));
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
            assertEquals("wholesale pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));

            context.removeSession("tenant-x");

            assertNull(context.currentChain("tenant-x"));
            assertEquals(retail, context.currentChain(), "the other scopes stay");
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testClosingAScopedSessionLeavesTheDefaultScopeAlone() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.initSession(new Req("retail"));
        try {
            try (var scope = ExtensionSessionScope.openScoped(context, "tenant-x", new Req("wholesale"))) {
                assertNotNull(scope);
                assertEquals("wholesale pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
            }

            assertNull(context.currentChain("tenant-x"));
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price), "closing the scoped session must not end the default one");
            assertEquals("retail", context.explain(Pricing.class).selected().code(), "explained from the chain bound to the scope, not from whatever resolved last");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRestoreScopedClosesOnlyItsScope() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            try (var scope = ExtensionSessionScope.restoreScoped(context, "tenant-x", wholesale)) {
                assertNotNull(scope);
                assertEquals("wholesale pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
            }
            assertNull(context.currentChain("tenant-x"));
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRegistryVersionTracksTheRegistry() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        String version = context.registryVersion();

        assertEquals(version, context.registryVersion(), "stable while nothing is registered");
        assertEquals(16, version.length());

        context.registerAbility(new ShopAbility("extra", r -> true, Pricing.class));
        String afterAbility = context.registryVersion();
        assertNotEquals(version, afterAbility);

        context.registerBusiness(ShopBusiness.named("distributor", Pricing.class));
        assertNotEquals(afterAbility, context.registryVersion());
    }

    @Test
    public void testRegistryVersionIsTheSameForTheSameRegistryWhateverTheOrder() throws Exception {
        DefaultExtensionContext<Req> a = new DefaultExtensionContext<>();
        ShopFixtures.emptyShop(a);
        a.registerExtensionPointDefaultImplementation(new FullDefaults());
        a.registerBusiness(ShopBusiness.named("one", Pricing.class));
        a.registerBusiness(ShopBusiness.named("two", Pricing.class));

        DefaultExtensionContext<Req> b = new DefaultExtensionContext<>();
        ShopFixtures.emptyShop(b);
        b.registerExtensionPointDefaultImplementation(new FullDefaults());
        b.registerBusiness(ShopBusiness.named("two", Pricing.class));
        b.registerBusiness(ShopBusiness.named("one", Pricing.class));

        assertEquals(a.registryVersion(), b.registryVersion());
        assertTrue(a.currentChain() == null && b.currentChain() == null);
    }
}
