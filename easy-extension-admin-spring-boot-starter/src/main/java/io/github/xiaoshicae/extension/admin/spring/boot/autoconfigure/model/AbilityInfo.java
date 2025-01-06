package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.util.List;

public class AbilityInfo {
    private final String code;
    private final List<String> implExtensionPoints;
    private final ClassInfo classInfo;

    public AbilityInfo(String code, List<String> implExtensionPoints, ClassInfo classInfo) {
        this.code = code;
        this.implExtensionPoints = implExtensionPoints;
        this.classInfo = classInfo;
    }

    public String getCode() {
        return code;
    }

    public List<String> getImplExtensionPoints() {
        return implExtensionPoints;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
