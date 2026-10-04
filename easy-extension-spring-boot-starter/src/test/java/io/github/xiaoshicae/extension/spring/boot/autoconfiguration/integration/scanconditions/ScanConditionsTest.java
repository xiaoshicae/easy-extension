package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanconditions;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code @Profile} and {@code @Conditional...} on scanned businesses must be evaluated against the environment of
 * the application context (profiles and properties of the application), like they are on any other component.
 */
public class ScanConditionsTest {

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

    /** Only for production. */
    @Business(code = "biz.prod")
    @Profile("prod")
    public static class ProdBusiness implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "prod";
        }
    }

    /** Everywhere but production. */
    @Business(code = "biz.notprod")
    @Profile("!prod")
    public static class NotProdBusiness implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "not prod";
        }
    }

    /** Behind a feature flag. */
    @Business(code = "biz.flag")
    @ConditionalOnProperty(name = "shop.feature.flag", havingValue = "true")
    public static class FlagBusiness implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return false;
        }

        @Override
        public String greet() {
            return "flag";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @SuppressWarnings("unchecked")
    private static List<String> businessCodes(AnnotationConfigApplicationContext ctx) {
        return ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream()
                .map(IBusiness::code).sorted().toList();
    }

    @Test
    public void testProfileOfScannedBusinessFollowsTheActiveProfilesOfTheApplication() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles("prod");
            ctx.register(Config.class);
            ctx.refresh();

            assertEquals(List.of("biz.prod"), businessCodes(ctx), "only the business of the active profile is registered");
        }
    }

    @Test
    public void testConditionalOnPropertyOfScannedBusinessReadsThePropertiesOfTheApplication() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("shop.feature.flag", "true")));
            ctx.register(Config.class);
            ctx.refresh();

            assertEquals(List.of("biz.flag", "biz.notprod"), businessCodes(ctx));
        }
    }
}
