package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.LiteWebJarsResourceResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

public class EasyExtensionWebMvcConfigurer implements WebMvcConfigurer {
    private final EasyExtensionAdminConfigurationProperties properties;
    private final EasyExtensionResourceResolver resourceResolver;
    private final String easyExtensionUIUrlPattern = "/easy-extension-admin-ui" + "*/**";
    private final String easyExtensionUIAPIPattern = "/easy-extension-api" + "*/**";
    private final String webjarLocation = "classpath:/META-INF/resources/webjars" + DEFAULT_PATH_SEPARATOR;
    private static final long MAX_AGE = 24 * 60 * 60;

    public EasyExtensionWebMvcConfigurer(EasyExtensionAdminConfigurationProperties properties) {
        this.properties = properties;
        this.resourceResolver = new EasyExtensionResourceResolver(properties);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String uiRootPath = properties.getPath();
        registry.addMapping(uiRootPath + easyExtensionUIAPIPattern)
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uiRootPath = properties.getPath();
        registry.addResourceHandler(uiRootPath + easyExtensionUIUrlPattern).addResourceLocations(webjarLocation).resourceChain(false).addResolver(resourceResolver);
        registry.addResourceHandler("/favicon.ico").addResourceLocations(webjarLocation).resourceChain(false).addResolver(resourceResolver);
    }

    private static class EasyExtensionResourceResolver extends LiteWebJarsResourceResolver {
        private final EasyExtensionAdminConfigurationProperties properties;

        public EasyExtensionResourceResolver(EasyExtensionAdminConfigurationProperties properties) {
            this.properties = properties;
        }

        @Override
        protected String findWebJarResourcePath(String pathStr) {
            String resourcePath = super.findWebJarResourcePath(pathStr);
            if (Objects.isNull(resourcePath)) {
                Path path = Paths.get(pathStr);
                if (path.getNameCount() < 2) {
                    return null;
                }
                Path first = path.getName(0);
                Path rest = path.subpath(1, path.getNameCount());
                return first.resolve(Consts.VERSION).resolve(rest).toString();
            }
            return resourcePath;
        }
    }
}
