package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.BusinessInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.MetadataJsonReader;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registries the core accepts must not make the admin fail: one odd business must not take the whole list down.
 */
public class ExtensionInfoServiceRobustnessTest {

    @MatcherParam
    public static class Param {
    }

    @ExtensionPoint
    public interface Pricing {
        String price();
    }

    public static class PricingDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Pricing {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class);
        }

        @Override
        public String price() {
            return "default price";
        }
    }

    /** A data-driven business: it mounts no abilities, and says so with {@code null} (the core accepts that). */
    public static class DataBusiness extends AbstractBusiness<Param> {
        @Override
        public String code() {
            return "biz.data";
        }

        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public Integer priority() {
            return 0;
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            return null;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of();
        }
    }

    private static ExtensionInfoService serviceFor(DefaultExtensionContext<Param> context) {
        return new ExtensionInfoService(context, new SourceCodeReader(new DefaultResourceLoader()),
                new MetadataJsonReader(), new EasyExtensionAdminConfigurationProperties());
    }

    @Test
    public void testBusinessWithoutUsedAbilitiesIsListed() throws Exception {
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Pricing.class);
        context.registerMatcherParamClass(Param.class);
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.registerBusiness(new DataBusiness());

        List<BusinessInfo> businesses = serviceFor(context).getAllBusiness();

        assertEquals(1, businesses.size());
        assertEquals("biz.data", businesses.get(0).code());
        assertTrue(businesses.get(0).usedAbilities().isEmpty());
    }

    @Test
    public void testMatcherParamOfAContextWithoutExtensionPointsIsEmptyInsteadOfFailing() {
        // what the starter registers when it finds no extension point: no matcher param class either
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();

        assertNotNull(serviceFor(context).getMatcherParamInfo().classInfo());
    }
}
