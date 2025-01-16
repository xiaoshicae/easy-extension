package io.github.xiaoshicae.extension.core.extension;

/**
 * Default implementation which implements all extension point, as a fallback when neither business nor used abilities have implemented specified extension point.
 * @param <T> matcher param class
 */
public abstract class AbstractExtensionPointDefaultImplementation<T> extends AbstractExtensionPointGroupImplementation<T> implements IExtensionPointGroupDefaultImplementation<T> {
    public static final String DEFAULT_CODE = "system.extension.point.default.implementation";

    private static final Integer PRIORITY = Integer.MAX_VALUE;

    @Override
    public final String code() {
        return DEFAULT_CODE;
    }

    @Override
    public final Boolean match(T param) {
        return true;
    }

    @Override
    public final Integer priority() {
        return PRIORITY;
    }
}
