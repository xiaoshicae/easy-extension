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

    /**
     * 认证配置。留空则不启用任何内置认证；应用仍可自行提供
     * {@code AdminAuthenticationProvider} bean 以接入 Spring Security 等。
     * Authentication config; leave empty to ship no built-in auth. Applications
     * may still register their own {@code AdminAuthenticationProvider} beans
     * (e.g. delegating to Spring Security).
     */
    private Auth auth = new Auth();

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

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth == null ? new Auth() : auth;
    }

    public static class Auth {
        /**
         * HTTP Basic 认证配置。只有当 {@code basic.username} 非空时才会注册
         * 内置的 Basic 认证 provider。
         */
        private Basic basic = new Basic();

        public Basic getBasic() {
            return basic;
        }

        public void setBasic(Basic basic) {
            this.basic = basic == null ? new Basic() : basic;
        }
    }

    public static class Basic {
        /** 用户名；留空禁用内置 Basic 认证。 */
        private String username = "";
        /** 密码；支持占位符 {@code ${...}}，建议从环境变量注入。 */
        private String password = "";
        /** WWW-Authenticate 中的 realm。 */
        private String realm = "Easy Extension Admin";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username == null ? "" : username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password == null ? "" : password;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }
    }
}
