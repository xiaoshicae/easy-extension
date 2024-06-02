package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultSession implements ISession {
    private final ThreadLocal<List<CodeWithPriority>> matchedCodeWithPriorityLocal = new ThreadLocal<>();

    private Boolean disablePriorityDuplicate = false;

    public DefaultSession() {}

    public DefaultSession(Boolean disablePriorityDuplicate) {
        this.disablePriorityDuplicate = disablePriorityDuplicate;
    }

    @Override
    public void setMatchedCode(String code, Integer priority) throws SessionException {
        List<CodeWithPriority> codeWithPriorities = matchedCodeWithPriorityLocal.get();
        if (codeWithPriorities == null) {
            codeWithPriorities = new ArrayList<>();
        }

        if (codeWithPriorities.stream().anyMatch(o-> Objects.equals(o.code, code))) {
            throw new SessionException("code " + code + " already exist");
        }

        if (disablePriorityDuplicate)  {
            CodeWithPriority conflictCodeWithPriority = codeWithPriorities.
                    stream().
                    filter(o -> Objects.equals(o.priority, priority)).
                    findFirst().
                    orElse(null);
            if (conflictCodeWithPriority != null) {
                throw new SessionException("priority equal conflict, (code " + code + " with priority " + priority + ") equal to (code " + conflictCodeWithPriority.code + " with priority " + conflictCodeWithPriority.priority + ")");
            }
        }

        codeWithPriorities.add(new CodeWithPriority(code, priority));
        codeWithPriorities.sort(Comparator.comparingInt(CodeWithPriority::getPriority));
        matchedCodeWithPriorityLocal.set(codeWithPriorities);
    }

    @Override
    public List<String> getMatchedCodes() throws SessionException {
        List<CodeWithPriority> codeWithPriorities = matchedCodeWithPriorityLocal.get();
        if (codeWithPriorities == null || codeWithPriorities.isEmpty()) {
            throw new SessionException("matched codes is empty, may be no code register");
        }
        return codeWithPriorities.stream().map(CodeWithPriority::getCode).collect(Collectors.toList());
    }

    @Override
    public void remove() {
        matchedCodeWithPriorityLocal.remove();
    }

    static class CodeWithPriority {
        private final String code;
        private final Integer priority;

        public CodeWithPriority(String code, Integer priority) {
            this.code = code;
            this.priority = priority;
        }

        public String getCode() {
            return code;
        }

        public Integer getPriority() {
            return priority;
        }
    }
}
