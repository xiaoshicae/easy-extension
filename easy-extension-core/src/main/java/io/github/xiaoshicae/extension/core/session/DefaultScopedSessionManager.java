package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class DefaultScopedSessionManager implements IScopedSessionManager {
    private final ThreadLocal<Map<String, TreeMap<Integer, String>>> scopedMatchedCodePriorityLocal = ThreadLocal.withInitial(HashMap::new);

    @Override
    public void setScopedMatchedCode(String scope, String code, Integer priority) throws SessionException {
        assertNotNull(scope, "scope");
        assertNotNull(code, "code");
        assertNotNull(priority, "priority");

        // get scoped matched code priority map
        TreeMap<Integer, String> codePriorityMap = scopedMatchedCodePriorityLocal.get().computeIfAbsent(scope, k -> new TreeMap<>());
        if (codePriorityMap.containsKey(priority)) {
            throw new SessionParamException(String.format("scope [%s], priority [%d] already exist", scope, priority));
        }
        // TODO: optimize performance
        if (codePriorityMap.containsValue(code)) {
            throw new SessionParamException(String.format("scope [%s], code [%s] already exist", scope, code));
        }
        codePriorityMap.put(priority, code);
    }

    @Override
    public List<String> getScopedMatchedCodes(String scope) throws SessionException {
        assertNotNull(scope, "scope");

        TreeMap<Integer, String> codePriorityMap = scopedMatchedCodePriorityLocal.get().get(scope);
        if (Objects.isNull(codePriorityMap) || codePriorityMap.isEmpty()) {
            throw new SessionNotFoundException(String.format("scope [%s], matched codes is empty, may be session not init", scope));
        }
        return codePriorityMap.values().stream().toList();
    }

    @Override
    public void removeAllSession() {
        scopedMatchedCodePriorityLocal.remove();
    }

    @Override
    public void removeScopedSession(String scope) {
        scopedMatchedCodePriorityLocal.get().remove(scope);
    }

    private void assertNotNull(Object obj, String paramName) throws SessionParamException {
        if (Objects.isNull(obj)) {
            throw new SessionParamException(paramName + " should not be null");
        }
    }
}
