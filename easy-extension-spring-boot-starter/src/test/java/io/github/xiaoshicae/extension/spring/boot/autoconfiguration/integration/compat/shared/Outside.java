package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.shared;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

/**
 * An extension point of a shared module: annotated, but its package is not one the application scans.
 */
@ExtensionPoint
public interface Outside {
    String outside();
}
