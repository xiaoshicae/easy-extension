package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cycle;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An ordinary application: a business needs a service, and the service calls an extension point; another business
 * calls an extension point itself. Spring Boot does not allow circular references by default
 * ({@code spring.main.allow-circular-references=false}), which is what {@code setAllowCircularReferences(false)} does.
 */
public class ExtensionInjectCycleTest {

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @ExtensionPoint
    public interface Pricing {
        String price();
    }

    @ExtensionPoint
    public interface Tax {
        String tax();
    }

    @MatcherParam
    public static class Param {
        final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    @ExtensionPointDefaultImplementation
    public static class Defaults implements Greeter, Pricing, Tax {
        @Override
        public String greet() {
            return "default greeting";
        }

        @Override
        public String price() {
            return "default price";
        }

        @Override
        public String tax() {
            return "default tax";
        }
    }

    /** A plain application service that calls the {@link Pricing} extension point. */
    @Component
    public static class PricingService {
        @ExtensionInject
        Pricing pricing;

        public String quote() {
            return pricing.price();
        }
    }

    /** Needs the service for its own work. */
    @Business(code = "biz.a")
    public static class BizA implements Matcher<Param>, Greeter {
        @Autowired
        PricingService pricingService;

        @Override
        public boolean match(Param param) {
            return "a".equals(param.name);
        }

        @Override
        public String greet() {
            return "a greets, " + pricingService.quote();
        }
    }

    /** Delegates to another extension point itself. */
    @Business(code = "biz.b")
    public static class BizB implements Matcher<Param>, Greeter {
        @ExtensionInject
        Tax tax;

        @Override
        public boolean match(Param param) {
            return "b".equals(param.name);
        }

        @Override
        public String greet() {
            return "b greets, " + tax.tax();
        }
    }

    @Configuration
    @ComponentScan
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @SuppressWarnings("unchecked")
    private static String greet(AnnotationConfigApplicationContext ctx, String name) throws Exception {
        IExtensionContext<Param> ec = ctx.getBean(IExtensionContext.class);
        ec.initSession(new Param(name));
        try {
            return ec.invoke(Greeter.class, Greeter::greet);
        } finally {
            ec.removeSession();
        }
    }

    @Test
    public void testBusinessThatNeedsAServiceThatCallsAnExtensionPointStartsWithoutCircularReferences() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.setAllowCircularReferences(false); // the Spring Boot default
            ctx.register(Config.class);
            ctx.refresh();

            assertEquals("a greets, default price", greet(ctx, "a"));
            assertEquals("b greets, default tax", greet(ctx, "b"));
        }
    }
}
