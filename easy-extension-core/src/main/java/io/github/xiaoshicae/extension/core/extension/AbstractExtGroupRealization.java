package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractExtGroupRealization<T> implements IExtGroupRealization<T> {

    public abstract String code();

    public abstract Boolean match(T param);

    @Override
    public List<Class<?>> implementsExtensions() {
        return Arrays.stream(this.getClass().getInterfaces()).filter((c) -> c.isAnnotationPresent(ExtensionPoint.class)).collect(Collectors.toList());
    }
}
