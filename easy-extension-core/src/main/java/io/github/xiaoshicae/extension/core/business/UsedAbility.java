package io.github.xiaoshicae.extension.core.business;


public class UsedAbility {
    /**
     * code of ability
     */
    private final String abilityCode;

    /**
     * priority of extension that implements by ability
     */
    private final int priority;

    public UsedAbility(String abilityCode, int priority) {
        this.abilityCode = abilityCode;
        this.priority = priority;
    }

    public String getAbilityCode() {
        return abilityCode;
    }

    public int getPriority() {
        return priority;
    }
}
