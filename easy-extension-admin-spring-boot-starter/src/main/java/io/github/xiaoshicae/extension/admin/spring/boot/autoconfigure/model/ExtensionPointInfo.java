package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;


public class ExtensionPointInfo {
    private final String id;
    private final ClassInfo classInfo;

    private final String defaultImplCode;

    public ExtensionPointInfo(ClassInfo classInfo, String defaultImplCode) {
        this.id = classInfo.getFullName();
        this.classInfo = classInfo;
        this.defaultImplCode = defaultImplCode;
    }

    public String  getId() {
        return id;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public String getDefaultImplCode() {
        return defaultImplCode;
    }
}
