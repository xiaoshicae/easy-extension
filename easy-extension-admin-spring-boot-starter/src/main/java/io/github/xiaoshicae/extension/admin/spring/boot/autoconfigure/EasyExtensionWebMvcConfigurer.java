package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.LiteWebJarsResourceResolver;


import java.util.List;

import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

/**
 * Configures web resources and CORS for the admin UI.
 * <p>
 * The admin UI frontend is always served from the "latest" directory,
 * regardless of the backend POM version. This ensures the admin UI
 * is always accessible after backend upgrades without needing to
 * rebuild the frontend.
 * </p>
 */
public class EasyExtensionWebMvcConfigurer implements WebMvcConfigurer {
    private final EasyExtensionAdminConfigurationProperties properties;
    private final EasyExtensionResourceResolver resourceResolver;
    /**
     * URL pattern for admin UI resources.
     * Matches paths like /easy-extension-admin-ui/latest/...
     */
    private final String easyExtensionUIUrlPattern = "/easy-extension-admin-ui/**";
    /**
     * URL pattern for admin API requests.
     */
    private final String easyExtensionUIAPIPattern = "/easy-extension-api/**";
    /**
     * Location of WebJar resources in the classpath.
     */
    private final String webjarLocation = "classpath:/META-INF/resources/webjars" + DEFAULT_PATH_SEPARATOR;
    private static final long MAX_AGE = 24 * 60 * 60;

    public EasyExtensionWebMvcConfigurer(EasyExtensionAdminConfigurationProperties properties) {
        this.properties = properties;
        this.resourceResolver = new EasyExtensionResourceResolver(properties);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String uiRootPath = properties.getPath();
        String[] origins = properties.getAllowedOrigins().toArray(new String[0]);
        registry.addMapping(uiRootPath + easyExtensionUIAPIPattern)
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(MAX_AGE);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uiRootPath = properties.getPath();
        // Point directly to the admin-ui webjar directory; no custom resolver needed.
        // no-cache: browser revalidates every request, JS/CSS with content hash still benefit from 304.
        String adminUiLocation = "classpath:/META-INF/resources/webjars/easy-extension-admin-ui/";
        registry.addResourceHandler(uiRootPath + easyExtensionUIUrlPattern)
                .addResourceLocations(adminUiLocation)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(false);
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/META-INF/resources/webjars/easy-extension-admin-ui/" + Consts.ADMIN_UI_VERSION + "/")
                .setCacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .resourceChain(false);
    }

    /**
     * Custom resource resolver that handles the "latest" version mapping.
     */
    private static class EasyExtensionResourceResolver extends LiteWebJarsResourceResolver {
        private final EasyExtensionAdminConfigurationProperties properties;

        public EasyExtensionResourceResolver(EasyExtensionAdminConfigurationProperties properties) {
            this.properties = properties;
        }

        @Override
        protected String findWebJarResourcePath(String pathStr) {
            String resourcePath = super.findWebJarResourcePath(pathStr);
            if (resourcePath != null) {
                return resourcePath;
            }
            // Fallback: try to resolve using the "latest" version directory.
            // Use string operations instead of java.nio.file.Path for cross-platform URL handling.
            String[] segments = pathStr.split("/");
            if (segments.length < 2) {
                return null;
            }
            // Insert the version segment after the first path segment (artifact name)
            StringBuilder sb = new StringBuilder(segments[0]);
            sb.append("/").append(Consts.ADMIN_UI_VERSION);
            for (int i = 1; i < segments.length; i++) {
                sb.append("/").append(segments[i]);
            }
            return sb.toString();
        }
    }
}
