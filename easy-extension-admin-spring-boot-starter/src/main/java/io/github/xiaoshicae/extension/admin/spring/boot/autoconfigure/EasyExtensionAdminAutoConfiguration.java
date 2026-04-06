package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth.AdminAuthenticationFilter;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth.AdminAuthenticationProvider;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service.ExtensionInfoService;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.MetadataJsonReader;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.IExtensionReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.ResourceLoader;

import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * Auto-configuration for the easy-extension admin module.
 * <p>
 * This configuration is only activated when:
 * <ul>
 *   <li>The application is a servlet-based web application</li>
 *   <li>The property {@code easy-extension.admin.enable} is not set to {@code false}</li>
 *   <li>An {@link IExtensionReader} bean is available in the context</li>
 * </ul>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(value = "easy-extension.admin.enable", matchIfMissing = true)
@AutoConfigureAfter(name = "io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration")
@EnableConfigurationProperties(EasyExtensionAdminConfigurationProperties.class)
public class EasyExtensionAdminAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SourceCodeReader sourceCodeReader(ResourceLoader resourceLoader) {
        return new SourceCodeReader(resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetadataJsonReader metadataJsonReader(ResourceLoader resourceLoader) {
        MetadataJsonReader reader = new MetadataJsonReader();
        reader.load(new org.springframework.core.io.support.PathMatchingResourcePatternResolver(resourceLoader));
        return reader;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(IExtensionReader.class)
    public ExtensionInfoService extensionInfoService(IExtensionReader<?> reader, SourceCodeReader sourceCodeReader,
                                                     MetadataJsonReader metadataReader, EasyExtensionAdminConfigurationProperties properties) {
        return new ExtensionInfoService(reader, sourceCodeReader, metadataReader, properties);
    }

    @Bean
    @ConditionalOnBean(ExtensionInfoService.class)
    public EasyExtensionAdminAPI easyExtensionAdminAPI(ExtensionInfoService extensionInfoService) {
        return new EasyExtensionAdminAPI(extensionInfoService);
    }

    @Bean
    @ConditionalOnMissingBean
    public EasyExtensionAdminHome easyExtensionAdminHome(EasyExtensionAdminConfigurationProperties properties) {
        return new EasyExtensionAdminHome(properties);
    }

    /**
     * Dynamically registers URL mappings for the admin home page based on the configured path.
     * This supports custom admin paths via {@code easy-extension.admin.path}.
     */
    @Bean
    public SimpleUrlHandlerMapping easyExtensionAdminHomeMapping(EasyExtensionAdminHome adminHome) {
        String path = adminHome.getRootPath();
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(path, adminHome, path + "/", adminHome));
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return mapping;
    }

    @Bean
    @ConditionalOnMissingBean
    public EasyExtensionWebMvcConfigurer easyExtensionWebMvcConfigurer(EasyExtensionAdminConfigurationProperties properties) {
        return new EasyExtensionWebMvcConfigurer(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler easyExtensionGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * Registers the authentication filter for admin API endpoints.
     * <p>
     * If no {@link AdminAuthenticationProvider} beans are present, the filter
     * passes all requests through (backward compatible).
     * </p>
     */
    @Bean
    public FilterRegistrationBean<AdminAuthenticationFilter> adminAuthenticationFilterRegistration(
            List<AdminAuthenticationProvider> providers,
            EasyExtensionAdminConfigurationProperties properties) {
        FilterRegistrationBean<AdminAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AdminAuthenticationFilter(providers, properties.getPath()));
        // Match all paths since the filter itself checks the admin API path
        registration.addUrlPatterns("/*");
        registration.setName("easyExtensionAdminAuthenticationFilter");
        // Set to high precedence so authentication happens before other filters
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
