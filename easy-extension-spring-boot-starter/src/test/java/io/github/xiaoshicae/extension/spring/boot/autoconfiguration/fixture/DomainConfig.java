package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stands in for the {@code @SpringBootApplication} class: its package is the auto-configuration package that gets scanned.
 */
@Configuration
@AutoConfigurationPackage
public class DomainConfig {

    @Bean
    public Domain.OrderService orderService() {
        return new Domain.OrderService();
    }
}
