package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

/**
 * Admin configuration information.
 *
 * @param docUrl  URL to the project documentation
 * @param version framework version number
 */
public record ConfigInfo(String docUrl, String version) {
}
