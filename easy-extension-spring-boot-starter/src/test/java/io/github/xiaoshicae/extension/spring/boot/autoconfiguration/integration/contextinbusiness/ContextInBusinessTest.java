package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.contextinbusiness;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The extension context is built from the businesses, so a business must not need the context while it is created
 * (Spring Boot refuses circular references by default). What it needs the context for happens when it is called, and
 * there are two ways to ask for it then.
 */
public class ContextInBusinessTest {

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @MatcherParam
    public static class Param {
        final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    @ExtensionPointDefaultImplementation
    public static class Defaults implements Greeter {
        @Override
        public String greet() {
            return "default greeting";
        }
    }

    /** Asks for the context as a lazy-resolution proxy. */
    @Business(code = "biz.lazy")
    public static class LazyBiz implements Matcher<Param>, Greeter {
        @Autowired
        @Lazy
        IExtensionContext<?> context;

        @Override
        public boolean match(Param param) {
            return "lazy".equals(param.name);
        }

        @Override
        public String greet() {
            return "lazy sees " + context.getLastResolveTrace().getMatchedBusinessCode();
        }
    }

    /** Asks for the context when it needs it. */
    @Business(code = "biz.provider")
    public static class ProviderBiz implements Matcher<Param>, Greeter {
        @Autowired
        ObjectProvider<IExtensionContext<?>> context;

        @Override
        public boolean match(Param param) {
            return "provider".equals(param.name);
        }

        @Override
        public String greet() {
            return "provider sees " + context.getObject().getLastResolveTrace().getMatchedBusinessCode();
        }
    }

    @Configuration
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
    public void testBusinessThatAsksForTheContextWhenItIsCalledStartsWithoutCircularReferences() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.setAllowCircularReferences(false); // the Spring Boot default
            ctx.register(Config.class);
            ctx.refresh();

            assertEquals("lazy sees biz.lazy", greet(ctx, "lazy"));
            assertEquals("provider sees biz.provider", greet(ctx, "provider"));
        }
    }
}
