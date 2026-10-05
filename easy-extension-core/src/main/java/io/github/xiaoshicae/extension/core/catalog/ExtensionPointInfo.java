package io.github.xiaoshicae.extension.core.catalog;

import java.util.List;

public record ExtensionPointInfo(Class<?> type, int version, List<String> scenarios, boolean optional) {
}
