package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.common.Identifier;
import io.github.xiaoshicae.extension.core.common.Matcher;

import java.util.List;

public interface IExtensionPointGroupImplementation<T> extends Matcher<T>, Identifier {

    /**
     * An implementation instance that implements group of extension point
     *
     * @return extension points that instance implements
     */
    List<Class<?>> implementExtensionPoints();
}
