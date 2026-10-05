package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture.IfaceConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture.IfaceParam;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture.IfacePay;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A class may carry {@code @Ability}/{@code @Business}/{@code @ExtensionPointDefaultImplementation}
 * and also implement the framework interface (e.g. extend {@code AbstractAbility}) to use the annotation
 * attributes such as {@code requires}/{@code excludes}. It must be registered exactly once, not twice
 * (once as an annotated instance and once as an {@code IAbility}/{@code IBusiness} bean).
 */
public class AnnotatedFrameworkInterfaceRegistrationTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testAnnotatedClassImplementingFrameworkInterfaceIsRegisteredOnce() throws Exception {
        try (AnnotationConfigApplicationContext spring = new AnnotationConfigApplicationContext(IfaceConfig.class)) {
            IExtensionContext extensionContext = spring.getBean(IExtensionContext.class);

            assertEquals(1, extensionContext.listAllAbility().size());
            assertEquals(1, extensionContext.listAllBusiness().size());
            assertNotNull(extensionContext.getExtensionPointDefaultImplementation());

            extensionContext.initSession(new IfaceParam());
            try {
                // business priority 10, mounted ability priority 5 (lower number wins), default implementation last
                List<IfacePay> all = extensionContext.getAllMatchedExtension(IfacePay.class);
                assertEquals(List.of("ability", "business", "default"), all.stream().map(IfacePay::pay).toList());
            } finally {
                extensionContext.removeSession();
            }
        }
    }
}
