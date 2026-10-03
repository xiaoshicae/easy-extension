package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScopedChainSessionTest {

    private static final ResolvedChain CHAIN = ResolvedChain.of("v1", List.of(
            new ResolutionEntry("biz", 0, EntryType.BUSINESS),
            new ResolutionEntry("ability", 1, EntryType.ABILITY),
            new ResolutionEntry("default", Integer.MAX_VALUE, EntryType.DEFAULT)));

    private final DefaultScopedSessionManager session = new DefaultScopedSessionManager();

    @AfterEach
    public void clear() {
        session.removeAllSession();
    }

    @Test
    public void testBoundChainIsServedAsIs() throws SessionException {
        session.bindScopedChain("xxx", CHAIN);

        assertTrue(session.hasScopedSession("xxx"));
        assertSame(CHAIN, session.getScopedChain("xxx"));
        assertEquals(List.of("biz", "ability", "default"), session.getScopedMatchedCodes("xxx"));
        assertSame(CHAIN.codes(), session.getScopedMatchedCodes("xxx"), "no copy on the lookup path");
    }

    @Test
    public void testBindingReplacesWhatTheScopeHeld() throws SessionException {
        session.setScopedMatchedCode("xxx", "old", 5);
        session.bindScopedChain("xxx", CHAIN);

        assertEquals(List.of("biz", "ability", "default"), session.getScopedMatchedCodes("xxx"));
    }

    @Test
    public void testScopesAreIndependent() throws SessionException {
        session.bindScopedChain("a", CHAIN);

        assertFalse(session.hasScopedSession("b"));
        assertNull(session.getScopedChain("b"));
        SessionException e = assertThrows(SessionException.class, () -> session.getScopedMatchedCodes("b"));
        assertEquals("scope [b], matched codes is empty, may be session not init", e.getMessage());

        session.removeScopedSession("a");
        assertNull(session.getScopedChain("a"));
        assertFalse(session.hasScopedSession("a"));
    }

    @Test
    public void testBindIsThreadConfined() throws Exception {
        session.bindScopedChain("xxx", CHAIN);

        AtomicReference<ResolvedChain> seenByOtherThread = new AtomicReference<>(CHAIN);
        Thread other = new Thread(() -> seenByOtherThread.set(session.getScopedChain("xxx")));
        other.start();
        other.join();

        assertNull(seenByOtherThread.get(), "a session is bound to the thread that initialized it");
        assertSame(CHAIN, session.getScopedChain("xxx"));
    }

    @Test
    public void testEntriesAddedToAChainSessionContinueFromTheChain() throws SessionException {
        session.bindScopedChain("xxx", CHAIN);

        session.setScopedMatchedCode("xxx", "extra", 5);
        assertEquals(List.of("biz", "ability", "extra", "default"), session.getScopedMatchedCodes("xxx"));
        assertNull(session.getScopedChain("xxx"), "no longer exactly the chain that was bound");

        SessionException e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode("xxx", "biz", 6));
        assertEquals("scope [xxx], code [biz] already exist", e.getMessage());
    }

    @Test
    public void testAccumulatedSessionKeepsServingTheSameListUntilItChanges() throws SessionException {
        session.setScopedMatchedCode("xxx", "a", 0);
        session.setScopedMatchedCode("xxx", "b", 1);

        List<String> first = session.getScopedMatchedCodes("xxx");
        assertSame(first, session.getScopedMatchedCodes("xxx"), "no copy per lookup");
        assertNull(session.getScopedChain("xxx"), "only a bound chain can be read back as a chain");

        session.setScopedMatchedCode("xxx", "c", 2);
        assertEquals(List.of("a", "b", "c"), session.getScopedMatchedCodes("xxx"));
        assertEquals(List.of("a", "b"), first, "a list handed out earlier does not change");
    }

    @Test
    public void testInvalidBindingIsRejected() {
        SessionException e = assertThrows(SessionParamException.class, () -> session.bindScopedChain(null, CHAIN));
        assertEquals("scope should not be null", e.getMessage());

        e = assertThrows(SessionParamException.class, () -> session.bindScopedChain("xxx", null));
        assertEquals("chain should not be null", e.getMessage());
    }

    /**
     * A session manager written against the interface as it was before chains existed: it only knows
     * {@code setScopedMatchedCode} and friends. The default methods must keep it working.
     */
    private static final class LegacyStore implements IScopedSessionManager {
        private final Map<String, TreeMap<Integer, String>> sessions = new HashMap<>();

        @Override
        public void setScopedMatchedCode(String scope, String code, Integer priority) {
            sessions.computeIfAbsent(scope, k -> new TreeMap<>()).put(priority, code);
        }

        @Override
        public List<String> getScopedMatchedCodes(String scope) throws SessionException {
            TreeMap<Integer, String> codes = sessions.get(scope);
            if (codes == null) {
                throw new SessionException("no session");
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
    public void testSessionManagerWithoutChainSupportStillBinds() throws SessionException {
        LegacyStore store = new LegacyStore();
        store.setScopedMatchedCode("xxx", "stale", 3);

        store.bindScopedChain("xxx", CHAIN);

        assertEquals(List.of("biz", "ability", "default"), store.getScopedMatchedCodes("xxx"), "bound by replaying the entries");
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> store.getScopedChain("xxx"));
        assertEquals("getScopedChain() is not supported by this session manager", e.getMessage());
    }
}
