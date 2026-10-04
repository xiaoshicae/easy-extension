package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlaptwoa;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlaptwob.ModuleB;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An application made of two modules, each with its own configuration class that carries {@code @ExtensionScan}.
 * Spring Boot does not allow bean definition overriding by default.
 */
public class TwoExtensionScansTest {

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @MatcherParam
    public static class Param {
    }

    @ExtensionPointDefaultImplementation
    public static class GreeterDefault implements Greeter {
        @Override
        public String greet() {
            return "default";
        }
    }

    @Business(code = "biz.a")
    public static class BizA implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "a";
        }
    }

    @Configuration
    @ExtensionScan
    public static class ModuleAConfig {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTwoConfigurationClassesWithExtensionScanStart() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.setAllowBeanDefinitionOverriding(false); // the Spring Boot default
            ctx.register(ModuleAConfig.class, ModuleB.ModuleBConfig.class, EasyExtensionAutoConfiguration.class);
            ctx.refresh();

            List<String> codes = ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream()
                    .map(IBusiness::code).sorted().toList();
            assertEquals(List.of("biz.a", "biz.b"), codes);
        }
    }
}
