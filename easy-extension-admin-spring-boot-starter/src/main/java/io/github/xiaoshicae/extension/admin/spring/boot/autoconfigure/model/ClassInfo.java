package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

public class ClassInfo {
    private final String name;
    private final String fullName;
    private final String sourceCode;
    private final String comment;

    public ClassInfo(String name, String fullName, String sourceCode, String comment) {
        this.name = name;
        this.fullName = fullName;
        this.sourceCode = sourceCode;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getComment() {
        return comment;
    }
}
