package io.github.xiaoshicae.extension.core.extension;


import java.util.List;

public interface IExtGroupRealization<T> {
    /**
     * code of instance that implements a group of extension
     *
     * @return code
     */
    String code();

    /**
     * instance is enabled
     *
     * @param param is used to check for a match with instance(i.e., business or ability)
     * @return is enabled
     */
    Boolean match(T param);


    /**
     * a group of extension interface (i.e., interface annotated with @Extension) implemented by the instance
     *
     * @return extension interfaces implemented by the group
     */
    List<Class<?>> implementsExtensions();
}
