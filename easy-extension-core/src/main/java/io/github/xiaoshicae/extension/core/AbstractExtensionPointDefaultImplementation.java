package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointGroupImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

/**
 * Default implementation of the extension, as a fallback when no business identity or capability matches.
 */
public abstract class AbstractExtensionPointDefaultImplementation<T> extends AbstractExtensionPointGroupImplementation<T> implements IExtensionPointGroupDefaultImplementation<T> {
    public static final String DEFAULT_CODE = "system.extension.point.default.implementation";

    private static final Integer PRIORITY = Integer.MAX_VALUE;

    @Override
    public final Boolean match(T param) {
        return true;
    }

    @Override
    public final String code() {
        return DEFAULT_CODE;
    }

    @Override
    public final Integer priority() {
        return PRIORITY;
    }
}
