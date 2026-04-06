package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

/**
 * Immutable record holding class information extracted from source code.
 *
 * @param name       simple class name
 * @param fullName   fully qualified class name
 * @param sourceCode the source code of the class (may be generated fallback if source unavailable)
 * @param comment    Javadoc comment extracted from source code
 */
public record ClassInfo(String name, String fullName, String sourceCode, String comment) {
}
