package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.interfaces.Identifier;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.List;

public interface IExtensionPointGroupImplementation<T> extends Matcher<T>, Identifier {

    /**
     * A group of extension point classes of the instance implements
     *
     * @return extension point classes of the instance implements
     */
    List<Class<?>> implementExtensionPoints();
}
