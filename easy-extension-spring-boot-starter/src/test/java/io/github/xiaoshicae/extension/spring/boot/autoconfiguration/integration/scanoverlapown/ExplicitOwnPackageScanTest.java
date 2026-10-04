package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlapown;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Naming a package that is scanned anyway (the package of the annotated class always is) must not fail the start.
 * Spring Boot does not allow bean definition overriding by default, which is what a package scanned twice amounts to.
 */
public class ExplicitOwnPackageScanTest {

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
    @ExtensionScan(scanPackages = "io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlapown")
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testScanPackagesThatNamesThePackageOfTheConfigurationClassStarts() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.setAllowBeanDefinitionOverriding(false); // the Spring Boot default
            ctx.register(Config.class);
            ctx.refresh();

            List<String> codes = ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream().map(IBusiness::code).toList();
            assertEquals(List.of("biz.a"), codes);
        }
    }
}
