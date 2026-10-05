package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanAbility;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture.ScanDefaultPay;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classes annotated with {@code @Ability} / {@code @ExtensionPointDefaultImplementation} are Spring beans:
 * the container must create each of them once, and the framework must call that very bean.
 */
public class InstanceScannerTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testAnnotatedClassesAreInstantiatedOnce() {
        ScanAbility.INSTANCES.set(0);
        ScanDefaultPay.INSTANCES.set(0);

        try (AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext(ScanConfig.class)) {
            assertEquals(1, ScanAbility.INSTANCES.get(), "@Ability class instantiation count");
            assertEquals(1, ScanDefaultPay.INSTANCES.get(), "@ExtensionPointDefaultImplementation class instantiation count");

            IExtensionContext extensionContext = spring.getBean(IExtensionContext.class);
            assertEquals(1, extensionContext.listAllAbility().size());
            IProxy descriptor = (IProxy) extensionContext.listAllAbility().get(0);
            assertSame(spring.getBean(ScanAbility.class), descriptor.getInstance(),
                    "the registered ability must be the Spring bean");
        }
    }
}
