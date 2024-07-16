package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.xiaoshicae.extension.core.DefaultExtContext;
import io.github.xiaoshicae.extension.core.IExtContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import java.util.*;

@Configuration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
public class EasyExtensionAutoConfiguration {

    @Bean
    public IExtContext<?> registerExtensionContext(EasyExtensionConfigurationProperties properties, List<IBusiness<?>> businesses, List<IAbility<?>> abilities) throws ExtensionException {
        Boolean enableLog = properties.getEnableLog();
        Boolean allowUnknownBusiness = properties.getAllowUnknownBusiness();

        DefaultExtContext<?> manager = new DefaultExtContext<>(enableLog, !allowUnknownBusiness);

        for (IBusiness business : businesses) {
            manager.registerBusiness(business);
        }

        for (IAbility ability : abilities) {
            manager.registerAbility(ability);
        }

        manager.validateContext();

        return manager;
    }
}
