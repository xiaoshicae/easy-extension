package io.github.xiaoshicae.extension.core.annotation;

/**
 * Position marker for {@link Business#abilities()}: where the business's own implementation ranks among the
 * abilities it mounts. It is not an ability itself: it only marks a slot in the {@code abilities} order, at most once,
 * and is never instantiated.
 */
public final class Self {
    private Self() {
    }
}
