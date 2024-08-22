package io.github.xiaoshicae.extension.core.priority;

public interface ICodePriority {

    /**
     * code of instance that implements a group of extension point
     *
     * @return code
     */
    String code();

    /**
     * instance be invoked priority, the smaller the value, the higher the priority
     *
     * @return priority
     */
    Integer priority();
}
