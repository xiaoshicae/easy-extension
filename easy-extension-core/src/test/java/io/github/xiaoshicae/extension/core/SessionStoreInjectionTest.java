package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Where the resolved chain of the current request lives is up to a {@link IScopedSessionManager}: the context takes it
 * as a constructor argument, so it can be held in something that follows the request across threads.
 */
public class SessionStoreInjectionTest {

    private static DefaultExtensionContext<Req> shop(IScopedSessionManager store) throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of(), store);
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(ShopBusiness.named("retail", Pricing.class));
        return context;
    }

    /** Keeps sessions in one shared map instead of a thread local: every thread sees them. */
    private static final class SharedStore implements IScopedSessionManager {
        private final Map<String, ResolvedChain> chains = new ConcurrentHashMap<>();
        private int binds;

        @Override
        public void setScopedMatchedCode(String scope, String code, Integer priority) {
            throw new UnsupportedOperationException("the context binds whole chains");
        }

        @Override
        public void bindScopedChain(String scope, ResolvedChain chain) {
            binds++;
            chains.put(scope, chain);
        }

        @Override
        public ResolvedChain getScopedChain(String scope) {
            return chains.get(scope);
        }

        @Override
        public List<String> getScopedMatchedCodes(String scope) throws SessionException {
            ResolvedChain chain = chains.get(scope);
            if (chain == null) {
                throw new SessionNotFoundException("no session in the shared store");
            }
            return chain.codes();
        }

        @Override
        public void removeScopedSession(String scope) {
            chains.remove(scope);
        }

        @Override
        public void removeAllSession() {
            chains.clear();
        }

        @Override
        public boolean hasScopedSession(String scope) {
            return chains.containsKey(scope);
        }
    }

    @Test
    public void testSessionLivesInTheInjectedStore() throws Exception {
        SharedStore store = new SharedStore();
        DefaultExtensionContext<Req> context = shop(store);

        context.initSession(new Req("retail"));

        assertEquals(1, store.binds);
        assertEquals(1, store.chains.size());
        assertSame(store.chains.values().iterator().next(), context.currentChain());

        context.removeSession();
        assertEquals(0, store.chains.size());
    }

    @Test
    public void testAStoreThatIsNotThreadBoundMakesTheSessionVisibleEverywhere() throws Exception {
        DefaultExtensionContext<Req> context = shop(new SharedStore());
        context.initSession(new Req("retail"));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertEquals("retail pricing", pool.submit(() -> context.invoke(Pricing.class, Pricing::price)).get());
        } finally {
            pool.shutdownNow();
            context.removeSession();
        }
    }

    @Test
    public void testTheDefaultStoreIsThreadBound() throws Exception {
        DefaultExtensionContext<Req> context = shop(new DefaultScopedSessionManager());
        context.initSession(new Req("retail"));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            assertNull(pool.submit(() -> context.currentChain()).get(), "another thread has no session of its own");
        } finally {
            pool.shutdownNow();
            context.removeSession();
        }
    }

    /**
     * A store written before chains existed: it only knows {@code setScopedMatchedCode} and friends.
     */
    private static final class LegacyStore implements IScopedSessionManager {
        private final Map<String, TreeMap<Integer, String>> sessions = new ConcurrentHashMap<>();

        @Override
        public void setScopedMatchedCode(String scope, String code, Integer priority) {
            sessions.computeIfAbsent(scope, k -> new TreeMap<>()).put(priority, code);
        }

        @Override
        public List<String> getScopedMatchedCodes(String scope) throws SessionException {
            TreeMap<Integer, String> codes = sessions.get(scope);
            if (codes == null) {
                throw new SessionNotFoundException("no session in the legacy store");
            }
            return new ArrayList<>(codes.values());
        }

        @Override
        public void removeScopedSession(String scope) {
            sessions.remove(scope);
        }

        @Override
        public void removeAllSession() {
            sessions.clear();
        }

        @Override
        public boolean hasScopedSession(String scope) {
            return sessions.containsKey(scope);
        }
    }

    @Test
    public void testStoreWithoutChainSupportStillWorks() throws Exception {
        DefaultExtensionContext<Req> context = shop(new LegacyStore());

        context.initSession(new Req("retail"));
        try {
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));

            // explain falls back to the trace of the latest resolution
            assertEquals("retail", context.explain(Pricing.class).selected().code());

            UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, context::currentChain);
            assertEquals("getScopedChain() is not supported by this session manager", e.getMessage());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testExplainFromTheTraceKnowsEveryDefault() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.REJECT, List.of(), new LegacyStore());
        ShopFixtures.emptyShop(context);
        context.addExtensionPointDefaultImplementation(new ShopFixtures.PricingDefault());
        context.addExtensionPointDefaultImplementation(new ShopFixtures.CustomDefault("shipping.fallback", 100));

        context.initSession(new Req("nobody"));
        try {
            assertEquals("shipping.fallback shipping", context.invoke(ShopFixtures.Shipping.class, ShopFixtures.Shipping::ship));
            assertEquals("shipping.fallback", context.explain(ShopFixtures.Shipping.class).selected().code(),
                    "explained as it is served, although this store keeps no chain");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testChainBoundToAStoreWithoutChainSupportIsReplayedEntryByEntry() throws Exception {
        LegacyStore store = new LegacyStore();
        DefaultExtensionContext<Req> context = shop(store);
        ResolvedChain chain = context.resolve(new Req("retail"));

        context.bind(chain);

        assertEquals(chain.codes(), store.getScopedMatchedCodes("__easy__extension__default__scope__"));
        List<ResolutionEntry> entries = chain.entries();
        assertEquals(entries.size(), store.sessions.get("__easy__extension__default__scope__").size());
    }

    @Test
    public void testStoreIsRequired() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new DefaultExtensionContext<Req>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of(), null));
        assertEquals("sessionManager should not be null", e.getMessage());
    }

    @Test
    public void testPoliciesAreRequired() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new DefaultExtensionContext<Req>(false, null, MultiMatchPolicy.REJECT, List.of()));
        assertEquals("unknownBusinessPolicy should not be null", e.getMessage());

        e = assertThrows(NullPointerException.class,
                () -> new DefaultExtensionContext<Req>(false, UnknownBusinessPolicy.REJECT, null, List.of()));
        assertEquals("multiMatchPolicy should not be null", e.getMessage());
    }
}
