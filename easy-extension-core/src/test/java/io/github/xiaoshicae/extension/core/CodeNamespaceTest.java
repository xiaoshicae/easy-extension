package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.CustomDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.Shipping;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopAbility;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A resolved chain holds one entry per code, whatever kind of implementation the code belongs to, and the extension
 * instances are looked up by code. So a business, an ability and a default implementation cannot share a code:
 * the chain would silently lose one of them (and its priority). Registration has to say so, in either order.
 */
public class CodeNamespaceTest {

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        return context;
    }

    @Test
    public void testBusinessCannotReuseTheCodeOfAnAbility() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.registerAbility(new ShopAbility("vip", r -> true, Shipping.class));

        // no extension point in common, so nothing else stops it
        RegisterDuplicateException e = assertThrows(RegisterDuplicateException.class, () -> context.registerBusiness(
                new ShopBusiness("vip", r -> true, 5, List.of(Pricing.class), List.of())));
        assertEquals("business code [vip] is already used by an ability", e.getMessage());
    }

    @Test
    public void testAbilityCannotReuseTheCodeOfABusiness() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.registerBusiness(new ShopBusiness("vip", r -> true, 5, List.of(Pricing.class), List.of()));

        RegisterDuplicateException e = assertThrows(RegisterDuplicateException.class, () -> context.registerAbility(
                new ShopAbility("vip", r -> true, Shipping.class)));
        assertEquals("ability code [vip] is already used by a business", e.getMessage());
    }

    @Test
    public void testDefaultImplementationCannotReuseTheCodeOfABusiness() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.addExtensionPointDefaultImplementation(new ShopFixtures.PricingDefault());
        context.registerBusiness(new ShopBusiness("shipping.fallback", r -> true, 5, List.of(Pricing.class), List.of()));

        RegisterDuplicateException e = assertThrows(RegisterDuplicateException.class,
                () -> context.addExtensionPointDefaultImplementation(new CustomDefault("shipping.fallback", 100)));
        assertEquals("default implementation code [shipping.fallback] is already used by a business", e.getMessage());
    }
}
