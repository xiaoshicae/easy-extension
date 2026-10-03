package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.TreeMap;

/**
 * Scoped session data holder.
 * <p>
 * Either holds a {@link ResolvedChain} that was bound as a whole (what the extension context does), or accumulates
 * code/priority pairs one at a time through {@link IScopedSessionManager#setScopedMatchedCode}. Either way the codes
 * are available as an immutable list that is built once, not on every lookup.
 * </p>
 * Accessed by a single thread only (it lives in a thread local), so it needs no synchronization.
 */
class ScopedSessionData {
    // only used when entries are added one at a time; a session bound as a chain never needs them
    private TreeMap<Integer, String> priorityToCodeMap;
    private Set<String> codeSet;
    private ResolvedChain chain;
    private List<String> codes;

    static ScopedSessionData of(ResolvedChain chain) {
        ScopedSessionData data = new ScopedSessionData();
        data.chain = chain;
        return data;
    }

    public boolean containsPriority(Integer priority) {
        materialize();
        return priorityToCodeMap.containsKey(priority);
    }

    public boolean containsCode(String code) {
        materialize();
        return codeSet.contains(code);
    }

    public void put(Integer priority, String code) {
        materialize();
        priorityToCodeMap.put(priority, code);
        codeSet.add(code);
        codes = null;
    }

    public List<String> getCodes() {
        if (chain != null) {
            return chain.codes();
        }
        if (codes == null) {
            codes = List.copyOf(priorityToCodeMap.values());
        }
        return codes;
    }

    /**
     * The chain this session was bound with, or {@code null} if it was accumulated entry by entry.
     */
    public ResolvedChain getChain() {
        return chain;
    }

    public boolean isEmpty() {
        return chain == null && (priorityToCodeMap == null || priorityToCodeMap.isEmpty());
    }

    /**
     * Entries are about to be added: set up the structures that hold them, starting from the entries of the chain
     * if the session was bound as one.
     */
    private void materialize() {
        if (priorityToCodeMap == null) {
            priorityToCodeMap = new TreeMap<>();
            codeSet = new HashSet<>();
        }
        if (chain == null) {
            return;
        }
        for (ResolutionEntry entry : chain.entries()) {
            priorityToCodeMap.put(entry.priority(), entry.code());
            codeSet.add(entry.code());
        }
        chain = null;
        codes = null;
    }
}

public class DefaultScopedSessionManager implements IScopedSessionManager {
    private final ThreadLocal<Map<String, ScopedSessionData>> scopedSessionDataLocal = ThreadLocal.withInitial(HashMap::new);

    @Override
    public void setScopedMatchedCode(String scope, String code, Integer priority) throws SessionException {
        assertNotNull(scope, "scope");
        assertNotNull(code, "code");
        assertNotNull(priority, "priority");

        ScopedSessionData sessionData = scopedSessionDataLocal.get().computeIfAbsent(scope, k -> new ScopedSessionData());
        if (sessionData.containsPriority(priority)) {
            throw new SessionParamException(String.format("scope [%s], priority [%d] already exist", scope, priority));
        }
        if (sessionData.containsCode(code)) {
            throw new SessionParamException(String.format("scope [%s], code [%s] already exist", scope, code));
        }
        sessionData.put(priority, code);
    }

    @Override
    public void bindScopedChain(String scope, ResolvedChain chain) throws SessionException {
        assertNotNull(scope, "scope");
        assertNotNull(chain, "chain");
        scopedSessionDataLocal.get().put(scope, ScopedSessionData.of(chain));
    }

    @Override
    public ResolvedChain getScopedChain(String scope) {
        ScopedSessionData sessionData = scopedSessionDataLocal.get().get(scope);
        return sessionData == null ? null : sessionData.getChain();
    }

    @Override
    public List<String> getScopedMatchedCodes(String scope) throws SessionException {
        assertNotNull(scope, "scope");

        ScopedSessionData sessionData = scopedSessionDataLocal.get().get(scope);
        if (sessionData == null || sessionData.isEmpty()) {
            throw new SessionNotFoundException(String.format("scope [%s], matched codes is empty, may be session not init", scope));
        }
        return sessionData.getCodes();
    }

    @Override
    public void removeAllSession() {
        scopedSessionDataLocal.remove();
    }

    @Override
    public void removeScopedSession(String scope) {
        scopedSessionDataLocal.get().remove(scope);
    }

    @Override
    public boolean hasScopedSession(String scope) {
        Map<String, ScopedSessionData> sessions = scopedSessionDataLocal.get();
        ScopedSessionData sessionData = sessions.get(scope);
        return sessionData != null && !sessionData.isEmpty();
    }

    private void assertNotNull(Object obj, String paramName) throws SessionParamException {
        if (obj == null) {
            throw new SessionParamException(paramName + " should not be null");
        }
    }
}
