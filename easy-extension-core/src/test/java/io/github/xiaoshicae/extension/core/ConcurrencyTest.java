package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.session.ExtensionSessionScope;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-cutting correctness tests for the features added in this refactor:
 * <ul>
 *   <li>ThreadLocal isolation under concurrent session usage</li>
 *   <li>{@link ExtensionSessionScope} always calls {@code removeSession()}</li>
 *   <li>{@link BusinessMatchSelector} customisation</li>
 *   <li>{@link IExtensionContext#explain(Class)} diagnostics</li>
 *   <li>Concurrent registration (Batch 1 unified concurrency)</li>
 * </ul>
 */
class ConcurrencyTest {

    private DefaultExtensionContext<Object> ctx;

    @BeforeEach
    void setUp() throws RegisterException {
        ctx = new DefaultExtensionContext<>(false, false);
        ctx.registerExtensionPoint(ExtA.class);
        ctx.registerExtensionPoint(ExtB.class);
        ctx.registerExtensionPoint(ExtC.class);
        ctx.registerExtensionPointDefaultImplementation(new ExtensionPointDefaultImplementation());
        ctx.registerAbility(new AbilityN());
        ctx.registerAbility(new AbilityNN());
        ctx.registerBusiness(new BusinessZZZ());
    }

    @Test
    void threadLocalSessionsAreIsolated() throws Exception {
        int threads = 16;
        int iterationsPerThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String param = (threadId % 2 == 0) ? "request-XXX" : "request-other";
                        try {
                            ctx.initSession(param);
                            ExtA a = ctx.getFirstMatchedExtension(ExtA.class);
                            String result = a.extA();
                            String expected = "request-XXX".equals(param)
                                    ? "BusinessZZZ extA"
                                    : "ExtDefaultAbility do extA";
                            if (!expected.equals(result)) {
                                failures.add("thread " + threadId + " iter " + i
                                        + " param=" + param + " got=" + result);
                            }
                        } finally {
                            ctx.removeSession();
                        }
                    }
                } catch (Throwable ex) {
                    failures.add("thread " + threadId + " threw: " + ex);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "pool did not finish in time");
        assertTrue(failures.isEmpty(), "routing mismatches: " + failures);
    }

    @Test
    void extensionSessionScopeAlwaysCleansUp() throws Exception {
        String result = ExtensionSessionScope.run(ctx, "request-XXX",
                () -> ctx.invoke(ExtA.class, ExtA::extA));
        assertEquals("BusinessZZZ extA", result);
        assertNull(ctx.getLastResolveTrace(), "session should be cleared after run()");

        // And on exception path
        RuntimeException boom = assertThrows(RuntimeException.class, () ->
                ExtensionSessionScope.<Object, Object>run(ctx, "request-XXX", () -> {
                    throw new RuntimeException("boom");
                })
        );
        assertEquals("boom", boom.getMessage());
        assertNull(ctx.getLastResolveTrace(), "session should be cleared even on failure");

        // try-with-resources form
        try (var scope = ExtensionSessionScope.open(ctx, "request-XXX")) {
            assertNotNull(ctx.getLastResolveTrace());
        }
        assertNull(ctx.getLastResolveTrace());
    }

    @Test
    void explainReturnsOrderedCandidatesAndPicksFirstImplementing() throws Exception {
        ctx.initSession("request-XXX");
        try {
            ExtensionExplanation<ExtA> e = ctx.explain(ExtA.class);
            assertEquals(ExtA.class, e.extensionPointType());
            assertTrue(e.candidates().size() >= 2, "expect business + default at minimum");
            // Candidates are ordered by resolution chain priority (ascending).
            for (int i = 1; i < e.candidates().size(); i++) {
                assertTrue(e.candidates().get(i - 1).priority() <= e.candidates().get(i).priority(),
                        "candidates should be priority-ordered");
            }
            assertNotNull(e.selected(), "BusinessZZZ implements ExtA so a winner must exist");
            assertEquals("BusinessZZZ", e.selected().code());
            assertTrue(e.selected().implementsExtensionPoint());

            // ExtC is implemented by AbilityN, AbilityNN, and the default. The
            // winner is whichever appears first in the priority-ordered chain.
            ExtensionExplanation<ExtC> c = ctx.explain(ExtC.class);
            assertNotNull(c.selected());
            assertTrue(c.selected().implementsExtensionPoint());
        } finally {
            ctx.removeSession();
        }
    }

    @Test
    void customBusinessMatchSelectorIsHonoured() throws Exception {
        var customCtx = new DefaultExtensionContext<Object>(false, false);
        customCtx.registerExtensionPoint(ExtA.class);
        customCtx.registerExtensionPoint(ExtB.class);
        customCtx.registerExtensionPoint(ExtC.class);
        customCtx.registerExtensionPointDefaultImplementation(new ExtensionPointDefaultImplementation());
        customCtx.registerAbility(new AbilityN());
        customCtx.registerAbility(new AbilityNN());
        // Two businesses that both match on "XXX"
        customCtx.registerBusiness(new BusinessZZZ());
        customCtx.registerBusiness(new BusinessZZZ2());

        // Default selector picks first-registered (BusinessZZZ)
        customCtx.initSession("XXX");
        try {
            assertEquals("BusinessZZZ", customCtx.getLastResolveTrace().getMatchedBusinessCode());
        } finally {
            customCtx.removeSession();
        }

        // Custom selector: prefer BusinessZZZ2 regardless of registration order
        customCtx.setBusinessMatchSelector((List<IBusiness<Object>> matched, Object param) -> {
            for (IBusiness<Object> b : matched) {
                if ("BusinessZZZ2".equals(b.code())) return b;
            }
            return matched.get(0);
        });
        customCtx.initSession("XXX");
        try {
            assertEquals("BusinessZZZ2", customCtx.getLastResolveTrace().getMatchedBusinessCode());
        } finally {
            customCtx.removeSession();
        }
    }

    @Test
    void concurrentRegistrationIsSafe() throws Exception {
        var freshCtx = new DefaultExtensionContext<Object>(false, false);
        freshCtx.registerExtensionPoint(ExtA.class);
        freshCtx.registerExtensionPoint(ExtB.class);
        freshCtx.registerExtensionPoint(ExtC.class);
        freshCtx.registerExtensionPointDefaultImplementation(new ExtensionPointDefaultImplementation());

        int registrants = 8;
        ExecutorService pool = Executors.newFixedThreadPool(registrants);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger duplicates = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        // All threads try to register the same two abilities. Exactly one
        // registration per code must succeed; the others must throw
        // RegisterException (duplicate), never a different exception.
        for (int i = 0; i < registrants; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    try {
                        freshCtx.registerAbility(new AbilityN());
                    } catch (RegisterException dup) {
                        duplicates.incrementAndGet();
                    }
                    try {
                        freshCtx.registerAbility(new AbilityNN());
                    } catch (RegisterException dup) {
                        duplicates.incrementAndGet();
                    }
                } catch (Throwable ex) {
                    unexpected.add(ex);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(unexpected.isEmpty(), "unexpected: " + unexpected);
        // 2 successful + (registrants*2 - 2) duplicates = 2*(registrants-1)
        assertEquals((registrants - 1) * 2, duplicates.get(),
                "each code must be registered exactly once across threads");

        // And the manager state is consistent with a single successful registration
        assertEquals(2, freshCtx.listAllAbility().size(),
                "exactly two abilities should be visible");
    }

    /** Second business that matches the same "XXX" param, used only for selector test. */
    static class BusinessZZZ2 extends io.github.xiaoshicae.extension.core.business.AbstractBusiness<Object>
            implements ExtA {
        @Override public String code() { return "BusinessZZZ2"; }
        @Override public boolean match(Object param) { return param.toString().contains("XXX"); }
        @Override public List<Class<?>> implementExtensionPoints() { return List.of(ExtA.class); }
        @Override public Integer priority() { return 101; }
        @Override public List<io.github.xiaoshicae.extension.core.business.UsedAbility> usedAbilities() {
            return new ArrayList<>();
        }
        @Override public String extA() { return "BusinessZZZ2 extA"; }
    }

    /** Ensure the IExtensionReader contract still holds after the concurrency refactor. */
    @Test
    void readerListsAreStable() throws Exception {
        List<?> firstRead = ctx.listAllAbility();
        List<?> secondRead = ctx.listAllAbility();
        assertEquals(firstRead.size(), secondRead.size());
        // Snapshot returned is independent — caller cannot mutate internal state
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<Object>) firstRead).add(null));
        assertSame(firstRead.size(), secondRead.size());
    }
}
