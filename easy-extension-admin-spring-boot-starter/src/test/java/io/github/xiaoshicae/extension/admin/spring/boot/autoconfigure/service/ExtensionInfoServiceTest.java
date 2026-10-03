package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ExtensionPointInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.MetadataJsonReader;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admin view of default implementations: there may be several (each for its own extension points), or none.
 */
public class ExtensionInfoServiceTest {

    public static class Param {
    }

    @ExtensionPoint
    public interface Pricing {
        String price();
    }

    @ExtensionPoint
    public interface Shipping {
        String ship();
    }

    /** No default for this one. */
    @ExtensionPoint(mandatory = true)
    public interface Tax {
        String tax();
    }

    @ExtensionPointDefaultImplementation
    public static class PricingDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Pricing {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class);
        }

        @Override
        public String price() {
            return "default pricing";
        }
    }

    @ExtensionPointDefaultImplementation
    public static class ShippingDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Shipping {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Shipping.class);
        }

        @Override
        public String ship() {
            return "default shipping";
        }
    }

    private static ExtensionInfoService serviceFor(DefaultExtensionContext<Param> context) {
        return new ExtensionInfoService(context, new SourceCodeReader(new DefaultResourceLoader()),
                new MetadataJsonReader(), new EasyExtensionAdminConfigurationProperties());
    }

    private static DefaultExtensionContext<Param> shop() throws Exception {
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Pricing.class);
        context.registerExtensionPoint(Shipping.class);
        context.registerExtensionPoint(Tax.class);
        context.registerMatcherParamClass(Param.class);
        return context;
    }

    @Test
    public void testEachExtensionPointShowsTheDefaultThatAnswersForIt() throws Exception {
        DefaultExtensionContext<Param> context = shop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new ShippingDefault());
        ExtensionInfoService service = serviceFor(context);

        Map<String, ExtensionPointInfo> byName = service.getAllExtensionPoints().stream()
                .collect(Collectors.toMap(info -> info.classInfo().name(), Function.identity()));

        assertTrue(byName.get("Pricing").defaultImplCode().contains("price()"), byName.get("Pricing").defaultImplCode());
        assertFalse(byName.get("Pricing").defaultImplCode().contains("ship()"));
        assertTrue(byName.get("Shipping").defaultImplCode().contains("ship()"), byName.get("Shipping").defaultImplCode());
        assertFalse(byName.get("Shipping").defaultImplCode().contains("price()"));
        assertEquals("", byName.get("Tax").defaultImplCode(), "a mandatory extension point has no default implementation");
    }

    @Test
    public void testDefaultImplInfoIsTheFirstDefault() throws Exception {
        DefaultExtensionContext<Param> context = shop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new ShippingDefault());

        assertEquals("PricingDefault", serviceFor(context).getDefaultImplInfo().classInfo().name());
    }

    @Test
    public void testNoDefaultImplementationAtAllIsFine() throws Exception {
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Tax.class);
        context.registerMatcherParamClass(Param.class);
        ExtensionInfoService service = serviceFor(context);

        assertEquals("", service.getDefaultImplInfo().classInfo().name());
        assertEquals("", service.getDefaultImplInfo().classInfo().sourceCode());
        assertEquals(1, service.getAllExtensionPoints().size());
        assertEquals("", service.getAllExtensionPoints().get(0).defaultImplCode());
    }
}
