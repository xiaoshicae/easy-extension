package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scannedandbeanmethod;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guard for the fix of "annotated beans that the scanner did not hand over are ignored": it passes today and has to
 * stay green. A class that the scanner registers is registered once, however many other beans of that class the
 * application defines, and a prototype is not instantiated again to look at it.
 */
public class ScannedAndBeanMethodBusinessTest {
    static final AtomicInteger PROTOTYPES_CREATED = new AtomicInteger();

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

    /** Found by the scanner, and defined once more by a {@code @Bean} method of the configuration. */
    @Business(code = "biz.both")
    public static class BothBusiness implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "both";
        }
    }

    @Scope("prototype")
    @Business(code = "biz.prototype")
    public static class PrototypeBusiness implements Matcher<Param>, Greeter {
        public PrototypeBusiness() {
            PROTOTYPES_CREATED.incrementAndGet();
        }

        @Override
        public boolean match(Param param) {
            return false;
        }

        @Override
        public String greet() {
            return "prototype";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
        @Bean
        BothBusiness anotherInstanceOfTheBusiness() {
            return new BothBusiness();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClassRegisteredByTheScannerIsRegisteredOnce() {
        PROTOTYPES_CREATED.set(0);
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            List<String> codes = ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream()
                    .map(IBusiness::code).sorted().toList();

            assertEquals(List.of("biz.both", "biz.prototype"), codes);
            assertEquals(1, PROTOTYPES_CREATED.get(), "the prototype is created once, for the registration");
        }
    }
}
