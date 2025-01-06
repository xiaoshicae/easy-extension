package io.github.xiaoshicae.extension.core.extension;

import java.util.List;

public abstract class AbstractExtensionPointGroupImplementation<T> implements IExtensionPointGroupImplementation<T> {

    public abstract String code();

    public abstract Boolean match(T param);

    public abstract List<Class<?>> implementExtensionPoints();
}
