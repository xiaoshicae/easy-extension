package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("easy-extension.admin")
public class EasyExtensionAdminConfigurationProperties {
    /**
     * 是否启用admin
     * admin is enabled
     */
    private Boolean enable = false;

    /**
     * 管理后台访问的path
     * path of admin
     */
    private String path = Consts.DEFAULT_ROOT_PATH;

    /**
     * 项目接入文档
     * document of project
     */
    private String docUrl = "";

    public Boolean getEnable() {
        return enable;
    }

    /**
     * @see enable
     */
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getPath() {
        return path;
    }

    /**
     * @see path
     */
    public void setPath(String path) {
        this.path = path;
    }

    public String getDocUrl() {
        return docUrl;
    }

    /**
     * @see docUrl
     */
    public void setDocUrl(String docUrl) {
        this.docUrl = docUrl;
    }
}
