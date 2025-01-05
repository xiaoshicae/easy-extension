package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.common.Identifier;
import io.github.xiaoshicae.extension.core.common.Priority;

public class UsedAbility implements Identifier, Priority {

    /**
     * Code of ability used by business.
     */
    private final String code;


    /**
     * Priority of ability used by business, compare with other ability or business priority.
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
