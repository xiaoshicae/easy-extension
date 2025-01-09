package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.common.Identifier;
import io.github.xiaoshicae.extension.core.common.Priority;

public class UsedAbility implements Identifier, Priority {

    /**
     * Code of ability used by business.
     */
    private final String code;


    /**
     * Priority of ability used by business,
     * compare with priority of other abilities used by business or business's own priority.
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
