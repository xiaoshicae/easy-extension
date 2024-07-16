package io.github.xiaoshicae.extension.core.business;


import io.github.xiaoshicae.extension.core.priority.ICodePriority;

public class UsedAbility implements ICodePriority {
    /**
     * code of ability
     */
    private final String code;

    /**
     * priority of extension that implements by ability
     */
    private final Integer priority;

    public UsedAbility(String code, Integer priority) {
        this.code = code;
        this.priority = priority;
    }

    public String code() {
        return code;
    }

    public Integer priority() {
        return priority;
    }
}
