package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class DefaultSession implements ISession {
    private final ThreadLocal<TreeMap<Integer, String>> matchedCodePriorityLocal = new ThreadLocal<>();

    @Override
    public void setMatchedCode(String code, Integer priority) throws SessionException {
        if (Objects.isNull(code)) {
            throw new SessionParamException("code should not be null");
        }
        if (Objects.isNull(priority)) {
            throw new SessionParamException("priority should not be null");
        }

        TreeMap<Integer, String> codePriorityMap = matchedCodePriorityLocal.get();
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
    public void remove() {
        matchedCodePriorityLocal.remove();
    }
}
