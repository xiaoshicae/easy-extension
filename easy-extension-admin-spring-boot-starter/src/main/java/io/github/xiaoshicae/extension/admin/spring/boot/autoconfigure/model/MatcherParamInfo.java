package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

public class MatcherParamInfo {
    private final ClassInfo classInfo;

    public MatcherParamInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
