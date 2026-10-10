package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.catalog.ExtensionPointInfo;
import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything registered, validated and immutable.
 */
record Registry<T>(Map<Class<?>, ExtensionPointInfo> points,
                   Map<String, AbilityEntry<T>> abilities,
                   Map<String, BusinessEntry<T>> businesses,
                   Map<Class<?>, DefaultEntry> defaults,
                   boolean strict,
                   ExtensionCatalog catalog) {

    record AbilityEntry<T>(String code, Matcher<T> matcher, Object impl, Class<?> userClass, Set<Class<?>> points,
                           List<String> requires, List<String> excludes) {
    }

    /**
     * @param mounts precedence order including the business itself
     */
    record BusinessEntry<T>(String code, Matcher<T> matcher, Object impl, Class<?> userClass, Set<Class<?>> points,
                            List<MountInfo> mounts) {
    }

    record DefaultEntry(Object impl, Class<?> userClass, Set<Class<?>> points) {
    }
}
