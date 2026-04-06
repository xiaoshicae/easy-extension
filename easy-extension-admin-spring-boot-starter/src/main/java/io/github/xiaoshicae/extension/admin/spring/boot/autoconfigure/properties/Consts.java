package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties;

/**
 * Constants for admin module.
 * <p>
 * The admin UI frontend resources are always stored in the "latest" directory
 * regardless of the POM version. This eliminates version mismatch issues and
 * ensures the admin UI is always accessible after backend upgrades.
 * </p>
 */
public class Consts {
    /**
     * The directory name for admin UI resources. Always "latest".
     * This is defined in easy-extension-admin-ui-frontend/build.sh
     */
    public static final String ADMIN_UI_VERSION = "latest";

    public static final String DEFAULT_ROOT_PATH = "/easy-extension-admin";

    /**
     * Default admin home path.
     */
    public static final String ADMIN_HOME_PATH = "/easy-extension-admin";

    /**
     * Default API URL suffix (relative to the admin home path).
     */
    public static final String API_URL_SUFFIX = "/easy-extension-api";

    public static final String ADMIN_UI_RESOURCE_HOME_PATH = "/easy-extension-admin-ui/" + ADMIN_UI_VERSION + "/index.html";

    /**
     * Default maximum pagination limit to prevent OOM attacks.
     */
    public static final int DEFAULT_MAX_PAGINATION_LIMIT = 500;
}
