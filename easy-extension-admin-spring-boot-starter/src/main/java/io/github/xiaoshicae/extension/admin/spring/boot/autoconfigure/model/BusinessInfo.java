package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

public class BusinessInfo {
    private final String code;
    private final Integer priority;
    private final List<UsedAbility> usedAbilities;
    private final List<String> implExtensionPoints;
    private final ClassInfo classInfo;


    public BusinessInfo(String code, Integer priority, List<UsedAbility> usedAbilities, List<String> implExtensionPoints, ClassInfo classInfo) {
        this.code = code;
        this.priority = priority;
        this.usedAbilities = usedAbilities;
        this.implExtensionPoints = implExtensionPoints;
        this.classInfo = classInfo;
    }

    public String getCode() {
        return code;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<UsedAbility> getUsedAbilities() {
        return usedAbilities;
    }

    public List<String> getImplExtensionPoints() {
        return implExtensionPoints;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public static class UsedAbility {
        private final String abilityCode;
        private final Integer priority;

        public UsedAbility(String abilityCode, Integer priority) {
            this.abilityCode = abilityCode;
            this.priority = priority;
        }

        public String getAbilityCode() {
            return abilityCode;
        }

        public Integer getPriority() {
            return priority;
        }
    }
}
