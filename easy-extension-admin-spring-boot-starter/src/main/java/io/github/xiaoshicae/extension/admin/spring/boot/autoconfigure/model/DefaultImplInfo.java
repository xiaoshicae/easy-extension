package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

/**
 * Default implementation information.
 *
 * @param classInfos class information of every default implementation (one class may back several extension points)
 */
public record DefaultImplInfo(List<ClassInfo> classInfos) {
}
