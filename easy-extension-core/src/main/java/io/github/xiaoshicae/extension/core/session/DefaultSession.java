package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class DefaultSession implements ISession {
    private final ThreadLocal<TreeMap<Integer, String>> matchedCodePriorityLocal = new ThreadLocal<>();
    private final ThreadLocal<Map<String, TreeMap<Integer, String>>> scopedMatchedCodePriorityLocal = new ThreadLocal<>();

    @Override
    public void setMatchedCode(String code, Integer priority) throws SessionException {
        TreeMap<Integer, String> codePriorityMap = matchedCodePriorityLocal.get();
        codePriorityMap = registerCodePriorityMap(code, priority, codePriorityMap);
        matchedCodePriorityLocal.set(codePriorityMap);
    }

    @Override
    public List<String> getMatchedCodes() throws SessionException {
        TreeMap<Integer, String> codePriorityMap = matchedCodePriorityLocal.get();
        if (Objects.isNull(codePriorityMap) || codePriorityMap.isEmpty()) {
            throw new SessionNotFoundException("matched codes is empty, may be no code register");
        }
        return codePriorityMap.values().stream().toList();
    }

    @Override
    public void setScopedMatchedCode(String scope, String code, Integer priority) throws SessionException {
        Map<String, TreeMap<Integer, String>> scopedCodePriorityMap = scopedMatchedCodePriorityLocal.get();
        scopedCodePriorityMap = registerScopedCodePriorityMap(scope, code, priority, scopedCodePriorityMap);
        scopedMatchedCodePriorityLocal.set(scopedCodePriorityMap);
    }

    @Override
    public List<String> getScopedMatchedCodes(String scope) throws SessionException {
        if (Objects.isNull(scope)) {
            throw new SessionParamException("scope should not be null");
        }
        Map<String, TreeMap<Integer, String>> scopedCodePriorityMap = scopedMatchedCodePriorityLocal.get();
        if (Objects.isNull(scopedCodePriorityMap) || scopedCodePriorityMap.isEmpty()) {
            throw new SessionNotFoundException(String.format("scope [%s], matched codes is empty, may be session not init", scope));
        }
        TreeMap<Integer, String> codePriorityMap = scopedCodePriorityMap.get(scope);
        if (Objects.isNull(codePriorityMap) || codePriorityMap.isEmpty()) {
            throw new SessionNotFoundException(String.format("scope [%s], matched codes is empty, may be no code register", scope));
        }
        return codePriorityMap.values().stream().toList();
    }

    @Override
    public void remove() {
        matchedCodePriorityLocal.remove();
        scopedMatchedCodePriorityLocal.remove();
    }

    private Map<String, TreeMap<Integer, String>> registerScopedCodePriorityMap(String scope, String code, Integer priority, Map<String, TreeMap<Integer, String>> scopedCodePriorityMap) throws SessionParamException {
        if (Objects.isNull(scope)) {
            throw new SessionParamException("scope should not be null");
        }
        if (Objects.isNull(scopedCodePriorityMap)) {
            scopedCodePriorityMap = new HashMap<>();
        }
        TreeMap<Integer, String> codePriorityMap = scopedCodePriorityMap.get(scope);
        try {
            codePriorityMap = registerCodePriorityMap(code, priority, codePriorityMap);
        } catch (SessionParamException e) {
            throw new SessionParamException("scope [%s], ".formatted(scope) + e.getMessage());
        }
        scopedCodePriorityMap.put(scope, codePriorityMap);
        return scopedCodePriorityMap;
    }

    private TreeMap<Integer, String> registerCodePriorityMap(String code, Integer priority, TreeMap<Integer, String> codePriorityMap) throws SessionParamException {
        if (Objects.isNull(code)) {
            throw new SessionParamException("code should not be null");
        }
        if (Objects.isNull(priority)) {
            throw new SessionParamException("priority should not be null");
        }

        if (codePriorityMap == null) {
            codePriorityMap = new TreeMap<>();
        }

        if (codePriorityMap.containsKey(priority)) {
            throw new SessionParamException(String.format("priority [%d] already exist", priority));
        }
        if (codePriorityMap.containsValue(code)) {
            throw new SessionParamException(String.format("code [%s] already exist", code));
        }

        codePriorityMap.put(priority, code);
        return codePriorityMap;
    }
}
