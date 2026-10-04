package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.CustomDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.PricingDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.Shipping;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopAbility;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A chain holds one entry per priority, and every entry needs one. Registration used to check neither against the
 * default implementations (or for {@code null}), so a registry that could never resolve a request was accepted, and each
 * request then failed with a message that did not say who was at fault.
 */
public class PriorityValidationTest {

    private static DefaultExtensionContext<Req> shopWithShippingFallbackAt100() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new CustomDefault("shipping.fallback", 100));
        return context;
    }

    @Test
    public void testBusinessWithoutPriorityIsRefused() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());

        ShopBusiness withoutPriority = new ShopBusiness("nameless", r -> true, 0, List.of(Pricing.class), List.of()) {
            @Override
            public Integer priority() {
                return null;
            }
        };
        RegisterParamException e = assertThrows(RegisterParamException.class, () -> context.registerBusiness(withoutPriority));
        assertEquals("business [nameless] priority should not be null", e.getMessage());
    }

    @Test
    public void testMountedAbilityWithoutPriorityIsRefused() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerAbility(new ShopAbility("ability.free", r -> true, Shipping.class));

        RegisterParamException e = assertThrows(RegisterParamException.class, () -> context.registerBusiness(
                new ShopBusiness("biz", r -> true, 0, List.of(Pricing.class), List.of(new UsedAbility("ability.free", null)))));
        assertEquals("business [biz] used ability [ability.free] priority should not be null", e.getMessage());
    }

    @Test
    public void testBusinessCannotTakeThePriorityOfADefaultImplementation() throws Exception {
        DefaultExtensionContext<Req> context = shopWithShippingFallbackAt100();
        context.registerBusiness(new ShopBusiness("clash", r -> true, 100, List.of(Pricing.class), List.of()));

        RegisterParamException e = assertThrows(RegisterParamException.class, context::validateRegistration);
        assertEquals("business [clash] has priority [100], which default implementation [shipping.fallback] has too", e.getMessage());
    }

    @Test
    public void testMountedAbilityCannotTakeThePriorityOfADefaultImplementation() throws Exception {
        DefaultExtensionContext<Req> context = shopWithShippingFallbackAt100();
        context.registerAbility(new ShopAbility("ability.last", r -> true, Shipping.class));
        context.registerBusiness(new ShopBusiness("biz", r -> true, 0, List.of(Pricing.class), List.of(new UsedAbility("ability.last", 100))));

        RegisterParamException e = assertThrows(RegisterParamException.class, context::validateRegistration);
        assertEquals("business [biz] mounts ability [ability.last] with priority [100], which default implementation [shipping.fallback] has too", e.getMessage());
    }

    @Test
    public void testResolutionNamesTheEntriesThatShareAPriority() throws Exception {
        // for whoever does not call validateRegistration(): the failure at least says who is at fault, and which scope
        // is only mentioned when it is a named one
        DefaultExtensionContext<Req> context = shopWithShippingFallbackAt100();
        context.registerBusiness(new ShopBusiness("clash", r -> true, 100, List.of(Pricing.class), List.of()));

        SessionException e = assertThrows(SessionException.class, () -> context.initSession(new Req("anybody")));
        assertEquals("priority [100] is taken by both business [clash] and default implementation [shipping.fallback]", e.getMessage());

        e = assertThrows(SessionException.class, () -> context.initSession("tenant-x", new Req("anybody")));
        assertEquals("scope [tenant-x], priority [100] is taken by both business [clash] and default implementation [shipping.fallback]", e.getMessage());
    }
}
