package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

/**
 * Business information returned to the admin UI.
 *
 * @param code                 unique code identifier of the business
 * @param priority             priority of the business
 * @param usedAbilities        list of abilities used by this business with their priorities
 * @param implExtensionPoints  list of extension point names this business implements
 * @param classInfo            class information of the business implementation
 */
public record BusinessInfo(String code, Integer priority, List<UsedAbility> usedAbilities,
                           List<String> implExtensionPoints, ClassInfo classInfo) {

    /**
     * Nested record representing an ability used by a business.
     *
     * @param abilityCode code of the used ability
     * @param priority    priority of the used ability
     */
    public record UsedAbility(String abilityCode, Integer priority) {
    }
}
