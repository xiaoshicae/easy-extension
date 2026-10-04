package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Auditor;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.BaseConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Closer;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.CountingSessionManager;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Greeter;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.InterceptorConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.PrimarySelectorConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.SecondSelectorConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.SecondSessionManagerConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.SelectorConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.SessionManagerConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How the starter turns beans and properties into a configured context.
 */
public class SpringWiringTest {

    @BeforeEach
    public void reset() {
        WiringFixtures.EVENTS.clear();
        CountingSessionManager.BINDS.set(0);
    }

    private static AnnotationConfigApplicationContext start(Map<String, Object> properties, Class<?>... configurations) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        ctx.register(configurations);
        ctx.refresh();
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private static IExtensionContext<Param> extensionContext(AnnotationConfigApplicationContext ctx) {
        return ctx.getBean(IExtensionContext.class);
    }

    private static String greet(IExtensionContext<Param> ec, String request) throws SessionException {
        ec.initSession(new Param(request));
        try {
            return ec.invoke(Greeter.class, Greeter::greet);
        } finally {
            ec.removeSession();
        }
    }

    @Test
    public void testDefaultImplementationsMaySplitAndExtensionPointsMayBeMandatory() throws Exception {
        try (var ctx = start(Map.of(), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals(2, ec.listExtensionPointDefaultImplementations().size(), "one default per extension point group");

            ec.initSession(new Param("a"));
            try {
                assertEquals("a greets", ec.invoke(Greeter.class, Greeter::greet));
                assertEquals("default closing", ec.invoke(Closer.class, Closer::close), "served by the second default");
                assertEquals("a audits", ec.invoke(Auditor.class, Auditor::audit));
            } finally {
                ec.removeSession();
            }

            ec.initSession(new Param("b"));
            try {
                InvokeException e = assertThrows(InvokeException.class, () -> ec.invoke(Auditor.class, Auditor::audit));
                assertTrue(e.getMessage().contains("it is a mandatory extension point"), e.getMessage());
            } finally {
                ec.removeSession();
            }
        }
    }

    @Test
    public void testPoliciesDefaultToTheStrictBehavior() throws Exception {
        try (var ctx = start(Map.of(), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("no business matched", assertThrows(SessionException.class, () -> ec.initSession(new Param("nobody"))).getMessage());
            assertEquals("multiple business found, matched business codes: [biz.a, biz.b]",
                    assertThrows(SessionException.class, () -> ec.initSession(new Param("both"))).getMessage());
        }
    }

    @Test
    public void testUnknownBusinessPolicyAloneLetsUnknownRequestsFallBackToTheDefaults() throws Exception {
        try (var ctx = start(Map.of("easy-extension.unknown-business-policy", "default"), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("default greeting", greet(ec, "nobody"));
            assertEquals("multiple business found, matched business codes: [biz.a, biz.b]",
                    assertThrows(SessionException.class, () -> ec.initSession(new Param("both"))).getMessage(), "several still fail");
        }
    }

    @Test
    public void testMultiMatchPolicyAloneLetsTheSelectorPickWhileUnknownStillFails() throws Exception {
        try (var ctx = start(Map.of("easy-extension.multi-match-policy", "select"), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "both"), "first registered wins");
            assertEquals("no business matched", assertThrows(SessionException.class, () -> ec.initSession(new Param("nobody"))).getMessage());
        }
    }

    @Test
    public void testAllowUnknownBusinessStillStandsForBothTogether() throws Exception {
        try (var ctx = start(Map.of("easy-extension.allow-unknown-business", "true"), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("default greeting", greet(ec, "nobody"));
            assertEquals("a greets", greet(ec, "both"));
        }
    }

    @Test
    public void testExplicitPolicyOverridesAllowUnknownBusiness() throws Exception {
        try (var ctx = start(Map.of("easy-extension.allow-unknown-business", "true", "easy-extension.multi-match-policy", "reject"), BaseConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("default greeting", greet(ec, "nobody"));
            assertEquals("multiple business found, matched business codes: [biz.a, biz.b]",
                    assertThrows(SessionException.class, () -> ec.initSession(new Param("both"))).getMessage());
        }
    }

    @Test
    public void testEffectivePoliciesOfThePropertiesObject() {
        EasyExtensionConfigurationProperties properties = new EasyExtensionConfigurationProperties();
        assertEquals("REJECT", properties.effectiveUnknownBusinessPolicy().name());
        assertEquals("REJECT", properties.effectiveMultiMatchPolicy().name());
        assertNull(properties.getUnknownBusinessPolicy());
        assertNull(properties.getMultiMatchPolicy());

        properties.setAllowUnknownBusiness(true);
        assertEquals("DEFAULT", properties.effectiveUnknownBusinessPolicy().name());
        assertEquals("SELECT", properties.effectiveMultiMatchPolicy().name());
    }

    @Test
    public void testInterceptorBeansWrapExtensionCallsInOrder() throws Exception {
        try (var ctx = start(Map.of(), BaseConfig.class, InterceptorConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "a"));

            assertEquals(List.of("first before", "second before", "second after", "first after"), WiringFixtures.EVENTS);
        }
    }

    @Test
    public void testSessionManagerBeanKeepsTheSessions() throws Exception {
        try (var ctx = start(Map.of(), BaseConfig.class, SessionManagerConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "a"));

            assertEquals(1, CountingSessionManager.BINDS.get());
        }
    }

    @Test
    public void testSeveralSessionManagerBeansDoNotStopTheApplicationFromStarting() throws Exception {
        // before 3.4 the starter did not look at session manager beans at all
        try (var ctx = start(Map.of(), BaseConfig.class, SessionManagerConfig.class, SecondSessionManagerConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "a"));

            assertEquals(0, CountingSessionManager.BINDS.get(), "which one is meant is not clear, so none is used");
        }
    }

    @Test
    public void testSeveralSelectorBeansDoNotStopTheApplicationFromStarting() throws Exception {
        try (var ctx = start(Map.of("easy-extension.multi-match-policy", "select"), BaseConfig.class, SelectorConfig.class, SecondSelectorConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "both"), "none of them is used: the first registered business wins as usual");
        }
    }

    @Test
    public void testPrimarySelectorBeanIsTheOneUsedAmongSeveral() throws Exception {
        try (var ctx = start(Map.of("easy-extension.multi-match-policy", "select"), BaseConfig.class, SelectorConfig.class, PrimarySelectorConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("a greets", greet(ec, "both"), "the primary selector picks the first, the other one would pick the last");
        }
    }

    @Test
    public void testBusinessMatchSelectorBeanDecidesBetweenSeveralMatches() throws Exception {
        try (var ctx = start(Map.of("easy-extension.multi-match-policy", "select"), BaseConfig.class, SelectorConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);

            assertEquals("b greets", greet(ec, "both"), "the selector bean picks the last one");
        }
    }
}
