package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.primary.PrimaryFixtures;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.scanned.ScannedFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Applications that started with 3.3 and must keep starting, whatever 3.4 reads out of their beans and classes.
 */
public class SpringUpgradeCompatibilityTest {

    private static AnnotationConfigApplicationContext start(Map<String, Object> properties, Class<?>... configurations) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        ctx.register(configurations);
        ctx.refresh();
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private static <P> IExtensionContext<P> extensionContext(AnnotationConfigApplicationContext ctx) {
        return ctx.getBean(IExtensionContext.class);
    }

    private static <P> String call(IExtensionContext<P> ec, P request, Function<IExtensionContext<P>, String> call) throws SessionException {
        ec.initSession(request);
        try {
            return call.apply(ec);
        } finally {
            ec.removeSession();
        }
    }

    @Test
    public void testApplicationStartsWhenItImplementsExtensionPointsOfAModuleItDoesNotScan() throws Exception {
        try (var ctx = start(Map.of("easy-extension.unknown-business-policy", "default"), ScannedFixtures.ScannedConfig.class)) {
            IExtensionContext<ScannedFixtures.Param> ec = extensionContext(ctx);

            // the business inherits Outside from its base class, the default implements it: neither is registered, neither matters
            assertEquals("inheriting greets", call(ec, new ScannedFixtures.Param("inheriting"),
                    e -> e.invoke(ScannedFixtures.Greeter.class, ScannedFixtures.Greeter::greet)));
            assertEquals("default greeting", call(ec, new ScannedFixtures.Param("nobody"),
                    e -> e.invoke(ScannedFixtures.Greeter.class, ScannedFixtures.Greeter::greet)));
        }
    }

    @Test
    public void testPrimaryDefaultImplementationBeanAloneIsTheDefault() throws Exception {
        try (var ctx = start(Map.of("easy-extension.unknown-business-policy", "default"), PrimaryFixtures.PrimaryConfig.class)) {
            IExtensionContext<PrimaryFixtures.Param> ec = extensionContext(ctx);

            assertEquals(1, ec.listExtensionPointDefaultImplementations().size(), "the other default implementation bean is ignored");
            assertEquals("caching default greeting", call(ec, new PrimaryFixtures.Param("nobody"),
                    e -> e.invoke(PrimaryFixtures.Greeter.class, PrimaryFixtures.Greeter::greet)));
            assertEquals("caching default closing", call(ec, new PrimaryFixtures.Param("nobody"),
                    e -> e.invoke(PrimaryFixtures.Closer.class, PrimaryFixtures.Closer::close)));
        }
    }

    @Test
    public void testDefaultImplementationBeansForDifferentExtensionPointsAreAllUsed() throws Exception {
        try (var ctx = start(Map.of("easy-extension.unknown-business-policy", "default"), PrimaryFixtures.SplitConfig.class)) {
            IExtensionContext<PrimaryFixtures.Param> ec = extensionContext(ctx);

            assertEquals(2, ec.listExtensionPointDefaultImplementations().size());
            assertEquals("greeter default", call(ec, new PrimaryFixtures.Param("nobody"),
                    e -> e.invoke(PrimaryFixtures.Greeter.class, PrimaryFixtures.Greeter::greet)));
            assertEquals("closer default", call(ec, new PrimaryFixtures.Param("nobody"),
                    e -> e.invoke(PrimaryFixtures.Closer.class, PrimaryFixtures.Closer::close)));
        }
    }
}
