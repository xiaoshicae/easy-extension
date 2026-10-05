package io.github.xiaoshicae.extension.spring.boot.defaultsfixture;

import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Default implementations without a dedicated class: a lambda {@code @Bean} and an automatic no-op for a void hook.
 */
@Configuration
@AutoConfigurationPackage
public class DefaultsConfig {

    @ExtensionPoint
    public interface Greeting {
        String greet(String name);
    }

    @ExtensionPoint
    public interface Hook {
        void run(List<String> trail);
    }

    @Bean
    @DefaultImplementation
    public Greeting defaultGreeting() {
        return name -> "hello " + name;
    }
}
