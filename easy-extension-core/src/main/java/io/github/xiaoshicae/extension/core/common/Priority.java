package io.github.xiaoshicae.extension.core.common;

@FunctionalInterface
public interface Priority {

    /**
     * Priority of extension point instance.
     *
     * @return priority
     */
    Integer priority();
}
