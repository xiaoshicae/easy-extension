package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.BusinessInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.MetadataJsonReader;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.Self;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionInfoServiceTest {

    @ExtensionPoint(scenarios = "shipping", version = 2)
    public interface Freight {
        int calc();
    }

    @DefaultImplementation
    public static class DefaultFreight implements Freight {
        public int calc() {
            return 8;
        }
    }

    @Ability(code = "ability.free")
    public static class Free implements Matcher<String>, Freight {
        public boolean match(String p) {
            return true;
        }

        public int calc() {
            return 0;
        }
    }

    @Business(code = "biz.retail", uses = {Free.class, Self.class})
    public static class Retail implements Matcher<String>, Freight {
        public boolean match(String p) {
            return true;
        }

        public int calc() {
            return 5;
        }
    }

    private ExtensionInfoService service() {
        ExtensionContext<String> ctx = ExtensionContext.<String>builder()
                .extensionPoint(Freight.class)
                .defaultImplementation(new DefaultFreight())
                .ability(new Free())
                .business(new Retail())
                .build();
        return new ExtensionInfoService(ctx, new SourceCodeReader(new DefaultResourceLoader()),
                new MetadataJsonReader(), new EasyExtensionAdminConfigurationProperties());
    }

    @Test
    public void testCatalogIsMappedToAdminModel() {
        ExtensionInfoService service = service();

        var points = service.getAllExtensionPoints();
        assertEquals(1, points.size());
        assertEquals(Freight.class.getName(), points.get(0).id());
        assertEquals(2, points.get(0).version());
        assertEquals(java.util.List.of("shipping"), points.get(0).scenarios());

        assertEquals("ability.free", service.getAllAbilities().get(0).code());
        assertEquals(1, service.getDefaultImplInfo().classInfos().size());
        assertEquals(String.class.getName(), service.getMatcherParamInfo().classInfo().fullName());
    }

    @Test
    public void testPriorityIsPositionInResolutionOrder() {
        BusinessInfo retail = service().getAllBusiness().get(0);

        assertEquals("biz.retail", retail.code());
        assertEquals(1, retail.priority());
        assertEquals(1, retail.usedAbilities().size());
        assertEquals("ability.free", retail.usedAbilities().get(0).abilityCode());
        assertEquals(0, retail.usedAbilities().get(0).priority());
    }
}
