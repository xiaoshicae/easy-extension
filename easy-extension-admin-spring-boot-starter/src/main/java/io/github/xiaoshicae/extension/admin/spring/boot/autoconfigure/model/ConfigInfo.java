package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

public class ConfigInfo {
    private final String docUrl;

    public ConfigInfo(String docUrl) {
        this.docUrl = docUrl;
    }

    public String getDocUrl() {
        return docUrl;
    }
}
