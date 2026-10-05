package io.github.xiaoshicae.extension.core.catalog;

import java.util.List;

/**
 * @param implementationClass the user's class (not a Spring proxy)
 * @param extensionPoints     extension points it implements
 * @param requires            codes of abilities that must be mounted together
 * @param excludes            codes of abilities that must not be mounted together
 */
public record AbilityInfo(String code, Class<?> implementationClass, List<Class<?>> extensionPoints,
                          List<String> requires, List<String> excludes) {
}
