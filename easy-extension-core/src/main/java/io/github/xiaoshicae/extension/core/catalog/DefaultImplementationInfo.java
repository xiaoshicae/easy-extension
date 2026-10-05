package io.github.xiaoshicae.extension.core.catalog;

import java.util.List;

public record DefaultImplementationInfo(Class<?> implementationClass, List<Class<?>> extensionPoints) {
}
