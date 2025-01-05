package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service.ExtensionInfoService;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty({"easy-extension.admin.enable"})
@EnableConfigurationProperties(EasyExtensionAdminConfigurationProperties.class)
public class EasyExtensionAdminAutoConfiguration {

    @Bean
    public EasyExtensionAdminHome registerAdminController(EasyExtensionAdminConfigurationProperties properties) {
        return new EasyExtensionAdminHome(properties);
    }

    @Bean
    public EasyExtensionAdminAPI registerAPIController(IExtensionContext<?> context, ResourceLoader resourceLoader, EasyExtensionAdminConfigurationProperties properties) {
        return new EasyExtensionAdminAPI(new ExtensionInfoService(context, new SourceCodeReader(resourceLoader), properties));
    }

    @Bean
    public EasyExtensionWebMvcConfigurer registerEasyExtensionWebMvcConfigurer(EasyExtensionAdminConfigurationProperties properties) {
        return new EasyExtensionWebMvcConfigurer(properties);
    }
}
