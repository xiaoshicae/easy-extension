package io.github.xiaoshicae.extension.processor;

import java.util.Map;

/**
 * 编译期提取的类元数据。
 *
 * @param className            简单类名
 * @param qualifiedName        全限定类名
 * @param annotationType       注解简称 (ExtensionPoint / Ability / Business / ExtensionPointDefaultImplementation)
 * @param sourceCode           完整源文件内容
 * @param javadoc              Javadoc 注释文本 (已去除 markers)
 * @param annotationAttributes 注解属性键值对
 */
public record ClassMetadata(
        String className,
        String qualifiedName,
        String annotationType,
        String sourceCode,
        String javadoc,
        Map<String, Object> annotationAttributes
) {
}
