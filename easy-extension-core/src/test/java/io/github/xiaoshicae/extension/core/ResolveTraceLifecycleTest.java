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
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.ExtensionSessionScope;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link IExtensionSession#getLastResolveTrace()} describes the session that is bound: it is there when a session is,
 * it is the trace of that session, and it goes away with the session, wherever that is removed.
 */
public class ResolveTraceLifecycleTest {

    /** "retail" mounts the ability "promo", which never matches; "wholesale" mounts nothing. */
    private static DefaultExtensionContext<Req> shop(IScopedSessionManager store) throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of(), store);
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerAbility(new ShopAbility("promo", r -> false, Pricing.class));
        context.registerBusiness(new ShopBusiness("retail", r -> "retail".equals(r.name), 0,
                List.of(Pricing.class, Tax.class), List.of(new UsedAbility("promo", 1))));
        context.registerBusiness(ShopBusiness.named("wholesale", Pricing.class, Tax.class));
        return context;
    }

    private static DefaultExtensionContext<Req> shop() throws Exception {
        return shop(new DefaultScopedSessionManager());
    }

    @Test
    public void testAFailedInitSessionLeavesNoTraceOfTheSessionItReplaced() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.initSession(new Req("retail"));
        try {
            SessionException e = assertThrows(SessionException.class, () -> context.initSession(new Req("nobody")));
            assertEquals("no business matched", e.getMessage());

            // initSession drops the session it replaces before it resolves; the trace must go with it
            assertNull(context.currentChain(), "the previous session is gone");
            assertNull(context.getLastResolveTrace(), "so is its trace: it is not the trace of anything that is bound");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testClosingAScopedSessionKeepsTheTraceOfTheDefaultSession() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.initSession(new Req("retail"));
        try {
            ResolveTrace traceOfTheDefaultSession = context.getLastResolveTrace();
            assertEquals("retail", traceOfTheDefaultSession.getMatchedBusinessCode());

            try (var scope = ExtensionSessionScope.openScoped(context, "tenant-x", new Req("wholesale"))) {
                assertNotNull(scope);
                assertEquals("wholesale", context.getLastResolveTrace().getMatchedBusinessCode());
            }

            assertNotNull(context.currentChain(), "the default session is still bound");
            assertSame(traceOfTheDefaultSession, context.getLastResolveTrace(), "and still explained by its own trace");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testAnInlineRunWithKeepsTheTraceOfTheThreadsOwnSession() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain wholesale = context.resolve(new Req("wholesale"));
        context.initSession(new Req("retail"));
        try {
            ResolveTrace before = context.getLastResolveTrace();
            assertEquals(1, before.getAbilities().size());
            assertEquals("ability.match() returned false", before.getAbilities().get(0).skippedReason());

            // a direct executor / CallerRunsPolicy: the task runs on the thread that has a session of its own
            ExtensionSessionScope.runWith(context, wholesale, () -> {
                assertEquals("wholesale", context.getLastResolveTrace().getMatchedBusinessCode());
            });

            assertSame(before, context.getLastResolveTrace(), "the session of the thread is back, with the trace that explains it");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testAChainBoundToAnotherScopeIsTracedUnderThatScope() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        ResolvedChain resolvedForTheDefaultScope = context.resolve(new Req("wholesale"));

        context.bind("tenant-x", resolvedForTheDefaultScope);
        try {
            ResolveTrace trace = context.getLastResolveTrace();
            assertEquals("tenant-x", trace.getScope());
            assertEquals("wholesale", trace.getMatchedBusinessCode());
        } finally {
            context.removeSession();
        }
    }

    /** Keeps sessions in one shared map instead of a thread local: what a store that follows the request does. */
    private static final class SharedStore implements IScopedSessionManager {
        private final Map<String, ResolvedChain> chains = new ConcurrentHashMap<>();

        @Override
        public void setScopedMatchedCode(String scope, String code, Integer priority) {
            throw new UnsupportedOperationException("the context binds whole chains");
        }

        @Override
        public void bindScopedChain(String scope, ResolvedChain chain) {
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
    public void testTheTraceIsRemovedWithTheSessionWhateverThreadRemovesIt() throws Exception {
        DefaultExtensionContext<Req> context = shop(new SharedStore());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            context.initSession(new Req("retail"));
            // the request moved on and was finished on another thread
            pool.submit(() -> context.removeSession()).get();

            assertNull(context.currentChain(), "the store no longer holds the session");
            assertNull(context.getLastResolveTrace(), "this thread keeps the trace of a request that is over");
        } finally {
            pool.shutdownNow();
            context.removeSession();
        }
    }

    @Test
    public void testTheTraceIsVisibleWhereverTheSessionIs() throws Exception {
        DefaultExtensionContext<Req> context = shop(new SharedStore());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            context.initSession(new Req("retail"));

            assertNotNull(context.currentChain());
            assertNotNull(pool.submit(() -> context.currentChain()).get(), "the session is visible on the other thread");
            assertNotNull(pool.submit(() -> context.getLastResolveTrace()).get(), "so is the trace that explains it");
        } finally {
            pool.shutdownNow();
            context.removeSession();
        }
    }
}
