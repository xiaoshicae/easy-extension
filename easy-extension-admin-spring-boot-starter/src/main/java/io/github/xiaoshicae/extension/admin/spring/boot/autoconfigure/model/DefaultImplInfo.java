package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

public class DefaultImplInfo {
    private final ClassInfo classInfo;

    public DefaultImplInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
