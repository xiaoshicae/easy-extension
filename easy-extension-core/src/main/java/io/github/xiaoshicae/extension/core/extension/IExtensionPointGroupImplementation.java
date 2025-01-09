package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.common.Identifier;
import io.github.xiaoshicae.extension.core.common.Matcher;

import java.util.List;

public interface IExtensionPointGroupImplementation<T> extends Matcher<T>, Identifier {

    /**
     * A group of extension point classes of the instance implements
     *
     * @return extension point classes of the instance implements
     */
    List<Class<?>> implementExtensionPoints();
}
