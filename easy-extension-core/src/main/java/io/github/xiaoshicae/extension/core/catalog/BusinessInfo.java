package io.github.xiaoshicae.extension.core.catalog;

import java.util.List;

/**
 * @param mounts precedence order, including the business itself (always present, see {@link MountInfo#isSelf()})
 */
public record BusinessInfo(String code, Class<?> implementationClass, List<Class<?>> extensionPoints,
                           List<MountInfo> mounts) {
}
