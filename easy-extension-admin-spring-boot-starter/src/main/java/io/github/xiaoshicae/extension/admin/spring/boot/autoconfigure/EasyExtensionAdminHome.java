package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import org.springframework.web.servlet.mvc.AbstractUrlViewController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller that redirects admin home path to the admin UI index page.
 * <p>
 * Supports dynamic path configuration via {@code easy-extension.admin.path}.
 * Uses {@link AbstractUrlViewController} pattern to handle URL-based routing.
 * </p>
 */
public class EasyExtensionAdminHome extends AbstractUrlViewController {
    private final String rootPath;
    private final String redirectUrl;

    public EasyExtensionAdminHome(EasyExtensionAdminConfigurationProperties properties) {
        String path = properties.getPath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Admin path must not be null or blank. Check 'easy-extension.admin.path' configuration.");
        }
        this.rootPath = path;
        this.redirectUrl = "redirect:" + rootPath + Consts.ADMIN_UI_RESOURCE_HOME_PATH;
    }

    public String getRootPath() {
        return rootPath;
    }

    @Override
    protected String getViewNameForRequest(HttpServletRequest request) {
        return redirectUrl;
    }
}
