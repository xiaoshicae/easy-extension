package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.BaseConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Greeter;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring.WiringFixtures.Param;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An {@link ExtensionInterceptor} is an ordinary bean: it may need the extension context (to ask which business the
 * request resolved to, for auditing, metrics tags, ...), the same way any other bean may.
 */
public class InterceptorBeanDependencyTest {

    @BeforeEach
    public void reset() {
        WiringFixtures.EVENTS.clear();
    }

    @Configuration
    public static class AuditingInterceptorConfig {
        @Bean
        ExtensionInterceptor auditing(IExtensionContext<?> context) {
            return invocation -> {
                WiringFixtures.EVENTS.add("audit " + context.getLastResolveTrace().getMatchedBusinessCode());
                return invocation.proceed();
            };
        }
    }

    private static void startAndCall(boolean allowCircularReferences) throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            // Spring Boot turns circular references off by default; a plain Spring context allows them
            ctx.setAllowCircularReferences(allowCircularReferences);
            ctx.register(BaseConfig.class, AuditingInterceptorConfig.class);
            ctx.refresh();

            @SuppressWarnings("unchecked")
            IExtensionContext<Param> ec = ctx.getBean(IExtensionContext.class);
            assertEquals(3, ec.listAllExtensionPoint().size(), "the extension points that were scanned are registered");
            assertEquals(2, ec.listAllBusiness().size(), "so are the businesses");
            ec.initSession(new Param("a"));
            try {
                assertEquals("a greets", ec.invoke(Greeter.class, Greeter::greet));
            } finally {
                ec.removeSession();
            }
        }
    }

    @Test
    public void testInterceptorBeanMayDependOnTheExtensionContextInAnApplicationThatAllowsCircularReferences() throws Exception {
        startAndCall(true);

        assertEquals(List.of("audit biz.a"), WiringFixtures.EVENTS);
    }

    @Test
    public void testInterceptorBeanMayDependOnTheExtensionContextInAnApplicationThatDoesNot() throws Exception {
        startAndCall(false);

        assertEquals(List.of("audit biz.a"), WiringFixtures.EVENTS);
    }
}
