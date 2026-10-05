package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.defaultsfixture.DefaultsConfig;
import io.github.xiaoshicae.extension.spring.boot.defaultsfixture.DefaultsConfig.Greeting;
import io.github.xiaoshicae.extension.spring.boot.defaultsfixture.DefaultsConfig.Hook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DefaultsTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class))
            .withUserConfiguration(DefaultsConfig.class)
            .withPropertyValues("easy-extension.allow-unknown-business=true");

    @Test
    public void testBeanMethodAndAutomaticNoOpDefaults() {
        runner.run(context -> {
            ExtensionContext<Object> extensionContext = context.getBean(ExtensionContext.class);
            var resolution = extensionContext.resolve(new Object());

            assertEquals("hello bob", resolution.first(Greeting.class).greet("bob"));
            List<String> trail = new ArrayList<>();
            resolution.first(Hook.class).run(trail);
            assertEquals(List.of(), trail);

            // only the @Bean default is an explicit default implementation
            assertEquals(1, extensionContext.catalog().defaultImplementations().size());
            assertEquals(2, extensionContext.catalog().extensionPoints().size());
        });
    }
}
