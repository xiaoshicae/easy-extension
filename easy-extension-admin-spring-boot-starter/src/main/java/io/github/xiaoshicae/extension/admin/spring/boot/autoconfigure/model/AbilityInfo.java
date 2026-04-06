package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

/**
 * Ability information returned to the admin UI.
 *
 * @param code                 unique code identifier of the ability
 * @param implExtensionPoints  list of extension point names this ability implements
 * @param classInfo            class information of the ability implementation
 */
public record AbilityInfo(String code, List<String> implExtensionPoints, ClassInfo classInfo) {
}
