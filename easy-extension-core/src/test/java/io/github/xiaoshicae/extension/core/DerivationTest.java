package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.catalog.BusinessInfo;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The extension points of an implementation are derived from its whole type hierarchy. In 3.x only the interfaces
 * declared directly on the class counted, so an implementation inherited from a superclass or reached through a
 * sub-interface was silently ignored and the default implementation answered instead.
 */
public class DerivationTest {

    public static abstract class BasePay implements Pay {
        @Override
        public String pay() {
            return "base-pay";
        }
    }

    @Business(code = "biz.via-superclass")
    public static class ViaSuperclass extends BasePay implements Matcher<Param>, Ship {
        @Override
        public boolean match(Param param) {
            return "superclass".equals(param.tenant());
        }

        @Override
        public String ship() {
            return "superclass-ship";
        }
    }

    /** Not annotated itself: it is an extension point only because it extends one. */
    public interface SpecialPay extends Pay {
    }

    @Business(code = "biz.via-sub-interface")
    public static class ViaSubInterface implements Matcher<Param>, SpecialPay {
        @Override
        public boolean match(Param param) {
            return "sub".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "special-pay";
        }
    }

    @ExtensionPoint
    public interface Premium extends Pay {
        String premium();
    }

    @Business(code = "biz.premium")
    public static class PremiumBusiness implements Matcher<Param>, Premium {
        @Override
        public boolean match(Param param) {
            return "premium".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "premium-pay";
        }

        @Override
        public String premium() {
            return "premium";
        }
    }

    @DefaultImplementation
    public static class DefaultPremium implements Premium {
        @Override
        public String pay() {
            return "default-premium-pay";
        }

        @Override
        public String premium() {
            return "default-premium";
        }
    }

    @Test
    public void testImplementationInheritedFromASuperclassIsNotLost() {
        ExtensionContext<Param> context = Fixtures.base().business(new ViaSuperclass()).build();

        Resolution resolution = context.resolve(Param.of("superclass"));
        assertEquals("base-pay", resolution.first(Pay.class).pay());
        assertEquals("superclass-ship", resolution.first(Ship.class).ship());
        assertEquals(Set.of(Pay.class, Ship.class), Set.copyOf(context.catalog().businesses().get(0).extensionPoints()));
    }

    @Test
    public void testImplementationReachedThroughASubInterfaceIsNotLost() {
        ExtensionContext<Param> context = Fixtures.base().business(new ViaSubInterface()).build();

        assertEquals("special-pay", context.resolve(Param.of("sub")).first(Pay.class).pay());
        assertEquals(List.of(Pay.class), context.catalog().businesses().get(0).extensionPoints());
    }

    @Test
    public void testImplementingAnExtensionPointAlsoImplementsItsParentExtensionPoint() {
        // PremiumBusiness implements Premium, which extends the extension point Pay: it answers both
        ExtensionContext<Param> context = ExtensionContext.<Param>builder()
                .extensionPoint(Pay.class, Premium.class)
                .defaultImplementation(new DefaultPremium())
                .business(new PremiumBusiness()).build();

        Resolution resolution = context.resolve(Param.of("premium"));
        assertEquals("premium-pay", resolution.first(Pay.class).pay());
        assertEquals("premium", resolution.first(Premium.class).premium());
        BusinessInfo info = context.catalog().businesses().get(0);
        assertEquals(Set.of(Pay.class, Premium.class), Set.copyOf(info.extensionPoints()));
    }

    @Test
    public void testAnExtensionPointInTheHierarchyMustBeRegistered() {
        // Premium extends the extension point Pay; registering only Pay leaves Premium unknown
        RegistrationException e = assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder()
                .extensionPoint(Pay.class).defaultImplementation(new DefaultPay()).business(new PremiumBusiness()).build());
        assertEquals("extension point [" + Premium.class.getName() + "] implemented by business [biz.premium] is not registered: "
                + "register it, or add its package to the scan", e.getMessage());
    }

    @Test
    public void testTwoDefaultImplementationsOfTheSameInheritedPointAreAmbiguous() {
        RegistrationException e = assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder()
                .extensionPoint(Pay.class, Premium.class)
                .defaultImplementation(new DefaultPay()).defaultImplementation(new DefaultPremium()).build());
        assertEquals("extension point [" + Pay.class.getName() + "] has more than one default implementation: ["
                + DefaultPay.class.getName() + "] and [" + DefaultPremium.class.getName() + "]", e.getMessage());
    }
}
