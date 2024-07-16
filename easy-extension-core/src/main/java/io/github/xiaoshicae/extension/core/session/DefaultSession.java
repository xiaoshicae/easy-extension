package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.*;

public class DefaultSession implements ISession {
    private final ThreadLocal<TreeMap<Integer, String>> matchedCodePriorityLocal = new ThreadLocal<>();

    @Override
    public void setMatchedCode(String code, Integer priority) throws SessionException {
        TreeMap<Integer, String> codePriorityMap = matchedCodePriorityLocal.get();
        if (codePriorityMap == null) {
            codePriorityMap = new TreeMap<>();
        }

        if (codePriorityMap.containsKey(priority)) {
            throw new SessionException("priority " + priority + " already exist");
        }

        if (codePriorityMap.containsValue(code)) {
            throw new SessionException("code " + code + " already exist");
        }

        codePriorityMap.put(priority, code);
        matchedCodePriorityLocal.set(codePriorityMap);
    }

    @Override
    public List<String> getMatchedCodes() throws SessionException {
        TreeMap<Integer, String> codePriorityMap = matchedCodePriorityLocal.get();
        if (codePriorityMap == null || codePriorityMap.isEmpty()) {
            throw new SessionException("matched codes is empty, may be no code register");
        }
        return codePriorityMap.values().stream().toList();
    }

    @Override
    public void remove() {
        matchedCodePriorityLocal.remove();
    }
}
