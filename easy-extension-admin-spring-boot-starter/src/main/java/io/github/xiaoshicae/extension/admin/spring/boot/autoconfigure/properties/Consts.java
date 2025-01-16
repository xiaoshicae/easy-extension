package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties;

public class Consts {
    public static final String VERSION = "3.0.7";

    public static final String DEFAULT_ROOT_PATH = "/easy-extension-admin";

    public static final String ADMIN_HOME_PATH = "${easy-extension.admin.path:#{T(io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts).DEFAULT_ROOT_PATH}}";

    public static final String API_URL_PREFIX = ADMIN_HOME_PATH + "/easy-extension-api";

    public static final String ADMIN_UI_RESOURCE_HOME_PATH = "/easy-extension-admin-ui/index.html";
}
