package io.github.xiaoshicae.extension.core.priority;

public interface ICodePriority {
    /**
     * code of extension implement
     *
     * @return code
     */
    String code();

    /**
     * Priority: The smaller the value, the higher the priority
     *
     * @return priority
     */
    Integer priority();
}
