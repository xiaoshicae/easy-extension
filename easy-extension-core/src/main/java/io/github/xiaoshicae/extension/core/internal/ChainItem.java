package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.trace.ResolveTrace;

import java.util.Set;

/**
 * One active member (the business or a matched ability) of a resolution chain.
 */
record ChainItem(String code, ResolveTrace.EntryType type, int position, Object impl, Class<?> userClass,
                 Set<Class<?>> points) {
}
