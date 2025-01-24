package io.github.xiaoshicae.extension.core.interfaces;

@FunctionalInterface
public interface Priority {

    /**
     * Priority of instance that implements a group of extension point.
     *
     * @return priority
     */
    Integer priority();
}
