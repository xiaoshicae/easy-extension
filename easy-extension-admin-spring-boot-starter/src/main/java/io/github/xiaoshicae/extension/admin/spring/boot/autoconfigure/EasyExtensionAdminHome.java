package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.web.servlet.view.UrlBasedViewResolver.REDIRECT_URL_PREFIX;

@Controller
public class EasyExtensionAdminHome {
    private final String rootPath;

    public EasyExtensionAdminHome(EasyExtensionAdminConfigurationProperties properties) {
        this.rootPath = properties.getPath();
    }

    @GetMapping({Consts.ADMIN_HOME_PATH})
    public String getAdminPage() {
        return REDIRECT_URL_PREFIX + rootPath + Consts.ADMIN_UI_RESOURCE_HOME_PATH;
    }
}
