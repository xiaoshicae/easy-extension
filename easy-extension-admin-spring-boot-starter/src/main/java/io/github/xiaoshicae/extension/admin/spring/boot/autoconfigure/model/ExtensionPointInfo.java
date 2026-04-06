package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

/**
 * Extension point information returned to the admin UI.
 *
 * @param id              fully qualified name of the extension point interface
 * @param classInfo       class information of the extension point
 * @param defaultImplCode source code of the default implementation filtered for this extension point
 * @param scenarios       applicable scenarios declared on @ExtensionPoint annotation
 * @param version         version number of the extension point interface
 */
public record ExtensionPointInfo(String id, ClassInfo classInfo, String defaultImplCode, List<String> scenarios, int version) {
    public ExtensionPointInfo(ClassInfo classInfo, String defaultImplCode, List<String> scenarios, int version) {
        this(classInfo.fullName(), classInfo, defaultImplCode, scenarios, version);
    }
}
