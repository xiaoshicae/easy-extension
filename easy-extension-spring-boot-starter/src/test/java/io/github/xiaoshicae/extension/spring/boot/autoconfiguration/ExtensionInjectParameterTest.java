package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanAbility;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanParam;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanPay;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @ExtensionInject} is declared for fields and parameters: constructor parameters must receive the
 * same session-aware proxies as fields do (not, for a List, every implementation bean of the type).
 */
public class ExtensionInjectParameterTest {

    public static class Consumer {
        final ScanPay single;
        final List<ScanPay> all;

        public Consumer(@ExtensionInject ScanPay single, @ExtensionInject List<ScanPay> all) {
            this.single = single;
            this.all = all;
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testConstructorParametersGetSessionAwareProxies() throws Exception {
        try (AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext()) {
            // the fixture has no business: let sessions fall back to the default implementation
            spring.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("easy-extension.allow-unknown-business", "true")));
            spring.register(ScanConfig.class, Consumer.class);
            spring.refresh();

            Consumer consumer = spring.getBean(Consumer.class);
            assertTrue(Proxy.isProxyClass(consumer.single.getClass()), "single parameter should be the first-matched proxy");
            assertTrue(Proxy.isProxyClass(consumer.all.getClass()), "list parameter should be the all-matched proxy");
            assertEquals("AllMatchedProxy[" + ScanPay.class.getName() + "]", consumer.all.toString());

            // only the default implementation is in the session (the ability is not mounted by any business)
            IExtensionContext extensionContext = spring.getBean(IExtensionContext.class);
            extensionContext.initSession(new ScanParam("any"));
            try {
                assertEquals(1, consumer.all.size());
                assertEquals("default", consumer.single.pay());
            } finally {
                extensionContext.removeSession();
            }
        }
    }

    public static class CollectionConsumer {
        final Set<ScanPay> set;

        public CollectionConsumer(@ExtensionInject Set<ScanPay> set) {
            this.set = set;
        }
    }

    @Test
    public void testUnsupportedParameterTypesKeepPlainSpringInjection() {
        // @ExtensionInject only defines "X" and "List<X>"; on any other type (Set, Collection, ...) parameters
        // used to be injected by Spring by type, and must keep starting up that way
        try (AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext()) {
            spring.register(ScanConfig.class, CollectionConsumer.class);
            spring.refresh();

            CollectionConsumer consumer = spring.getBean(CollectionConsumer.class);
            assertTrue(consumer.set.contains(spring.getBean(ScanAbility.class)));
        }
    }
}
