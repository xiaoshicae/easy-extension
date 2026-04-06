package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties("easy-extension.admin")
public class EasyExtensionAdminConfigurationProperties {
    /**
     * 是否启用admin
     * admin is enabled
     */
    private Boolean enable = true;

    /**
     * 管理后台访问的path
     * path of admin
     */
    @NotBlank(message = "Admin path must not be blank")
    private String path = "/easy-extension-admin";

    /**
     * 项目接入文档
     * document of project
     */
    private String docUrl = "";

    /**
     * 允许跨域的源 (CORS allowed origins). 默认为 ["*"]，生产环境建议配置具体域名
     * allowed origins for CORS. defaults to ["*"], recommend specifying concrete domains in production
     */
    private List<String> allowedOrigins = List.of("*");

    /**
     * 扩展点在管理后台的展示顺序，使用类的简单名称（SimpleName）。
     * 未列出的扩展点按注册顺序排在末尾。
     * <p>
     * Display order of extension points in the admin UI, using class simple names.
     * Unlisted extension points are appended at the end in their registration order.
     * </p>
     */
    private List<String> extensionPointOrder = List.of();

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
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Admin path must not be null or blank");
        }
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

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getExtensionPointOrder() {
        return extensionPointOrder;
    }

    public void setExtensionPointOrder(List<String> extensionPointOrder) {
        this.extensionPointOrder = extensionPointOrder;
    }
}
