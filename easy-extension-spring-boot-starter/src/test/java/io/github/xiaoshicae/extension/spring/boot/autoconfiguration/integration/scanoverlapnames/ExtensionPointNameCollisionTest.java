package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlapnames;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The scanner never registers a bean under the default name of an extension point interface, so an unrelated bean
 * that happens to carry that name must not make the application fail to start.
 */
public class ExtensionPointNameCollisionTest {

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

    /** An application service that is named after the interface. */
    @Service("greeter")
    public static class GreeterService {
    }

    @Configuration
    @ComponentScan
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBeanNamedLikeAnExtensionPointDoesNotStopTheApplicationFromStarting() {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            List<String> codes = ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream().map(IBusiness::code).toList();
            assertEquals(List.of("biz.a"), codes);
        }
    }
}
