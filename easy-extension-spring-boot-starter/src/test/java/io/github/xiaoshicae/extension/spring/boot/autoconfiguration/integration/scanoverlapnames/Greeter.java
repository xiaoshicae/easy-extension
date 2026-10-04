package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlapnames;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

/** A top-level extension point: the name Spring would give it as a bean is "greeter". */
@ExtensionPoint
public interface Greeter {
    String greet();
}
