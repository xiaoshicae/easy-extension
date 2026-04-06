package io.github.xiaoshicae.extension.intellij.util;

/**
 * Easy-Extension 框架注解的全限定名常量
 */
public final class EasyExtensionAnnotations {

    public static final String EXTENSION_POINT =
            "io.github.xiaoshicae.extension.core.annotation.ExtensionPoint";

    public static final String ABILITY =
            "io.github.xiaoshicae.extension.core.annotation.Ability";

    public static final String BUSINESS =
            "io.github.xiaoshicae.extension.core.annotation.Business";

    public static final String DEFAULT_IMPLEMENTATION =
            "io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation";

    public static final String EXTENSION_INJECT =
            "io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject";

    private EasyExtensionAnnotations() {
    }
}
