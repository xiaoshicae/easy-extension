package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.Pay;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.RetailBusiness;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.Ship;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.DomainConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @ExtensionInject} is declared for fields and parameters: constructor parameters must receive the same
 * session-aware proxies as fields do (not, for a List, every implementation bean of the type).
 */
public class ExtensionInjectParameterTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class))
            .withUserConfiguration(DomainConfig.class);

    public static class Consumer {
        final Ship single;
        final List<Pay> all;

        public Consumer(@ExtensionInject Ship single, @ExtensionInject List<Pay> all) {
            this.single = single;
            this.all = all;
        }
    }

    public static class CollectionConsumer {
        final Set<Pay> set;

        public CollectionConsumer(@ExtensionInject Set<Pay> set) {
            this.set = set;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConstructorParametersGetSessionAwareProxies() {
        runner.withUserConfiguration(Consumer.class).run(context -> {
            Consumer consumer = context.getBean(Consumer.class);
            assertTrue(Proxy.isProxyClass(consumer.single.getClass()), "single parameter should be the proxy");
            assertEquals("ExtensionProxyList[" + Pay.class.getName() + "]", consumer.all.toString(),
                    "list parameter should be the all-matched view, not the list of every Pay bean");

            ExtensionContext<Param> extensionContext = context.getBean(ExtensionContext.class);
            try (Binding ignored = extensionContext.bind(new Param("retail"))) {
                assertEquals("fast-ship", consumer.single.ship());
                assertEquals(List.of("retail-pay", "default-pay"), consumer.all.stream().map(Pay::pay).toList());
            }
        });
    }

    @Test
    public void testUnsupportedParameterTypesKeepPlainSpringInjection() {
        // @ExtensionInject only defines "X" and "List<X>"; on any other type (Set, Collection, ...) parameters
        // used to be injected by Spring by type, and must keep starting up that way
        runner.withUserConfiguration(CollectionConsumer.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBean(CollectionConsumer.class).set.contains(context.getBean(RetailBusiness.class)));
        });
    }
}
