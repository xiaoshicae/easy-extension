package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.Shipping;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopAbility;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An application has hundreds of businesses and abilities, and registration fails at startup: the message has to say
 * which of them it is about, not only which extension point is missing.
 */
public class RegistrationMessageTest {

    private static DefaultExtensionContext<Req> contextWithPricingOnly() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Pricing.class);
        context.registerMatcherParamClass(Req.class);
        return context;
    }

    @Test
    public void testUnregisteredExtensionPointNamesTheBusinessThatImplementsIt() throws Exception {
        DefaultExtensionContext<Req> context = contextWithPricingOnly();

        RegisterException e = assertThrows(RegisterException.class,
                () -> context.registerBusiness(new ShopBusiness("biz.retail", r -> true, 0, List.of(Pricing.class, Shipping.class), List.of())));
        assertEquals("extension point [" + Shipping.class.getName() + "] not registered, business [biz.retail] implements it", e.getMessage());
    }

    @Test
    public void testUnregisteredExtensionPointNamesTheAbilityThatImplementsIt() throws Exception {
        DefaultExtensionContext<Req> context = contextWithPricingOnly();

        RegisterException e = assertThrows(RegisterException.class,
                () -> context.registerAbility(new ShopAbility("ability.free-shipping", r -> true, Shipping.class)));
        assertEquals("extension point [" + Shipping.class.getName() + "] not registered, ability [ability.free-shipping] implements it", e.getMessage());
    }
}
