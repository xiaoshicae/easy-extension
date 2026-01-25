package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Scoped session data holder.
 * Holds both priority-to-code mapping and code set for O(1) lookup.
 */
class ScopedSessionData {
    private final TreeMap<Integer, String> priorityToCodeMap = new TreeMap<>();
    private final Set<String> codeSet = new HashSet<>();

    public boolean containsPriority(Integer priority) {
        return priorityToCodeMap.containsKey(priority);
    }

    public boolean containsCode(String code) {
        return codeSet.contains(code);
    }

    public void put(Integer priority, String code) {
        priorityToCodeMap.put(priority, code);
        codeSet.add(code);
    }

    public List<String> getCodes() {
        return priorityToCodeMap.values().stream().toList();
    }

    public boolean isEmpty() {
        return priorityToCodeMap.isEmpty();
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
    public List<String> getScopedMatchedCodes(String scope) throws SessionException {
        assertNotNull(scope, "scope");

        ScopedSessionData sessionData = scopedSessionDataLocal.get().get(scope);
        if (Objects.isNull(sessionData) || sessionData.isEmpty()) {
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

    private void assertNotNull(Object obj, String paramName) throws SessionParamException {
        if (Objects.isNull(obj)) {
            throw new SessionParamException(paramName + " should not be null");
        }
    }
}
