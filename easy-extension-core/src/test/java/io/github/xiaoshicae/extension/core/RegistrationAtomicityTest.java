package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopAbility;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A registration that is refused leaves nothing behind (the tests of the default implementations already hold the
 * registry to that). A business or an ability used to be stored first and wired to its extension points second, so a
 * failure in the second step left it in the registry, matching requests, without the instances it answers for.
 */
public class RegistrationAtomicityTest {

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        return context;
    }

    @Test
    public void testRefusedBusinessLeavesNothingBehind() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        String versionBefore = context.registryVersion();

        // the same extension point twice: refused when the second one is wired, after the first one was
        RegisterDuplicateException e = assertThrows(RegisterDuplicateException.class, () -> context.registerBusiness(
                new ShopBusiness("twice", r -> true, 5, List.of(Pricing.class, Pricing.class), List.of())));
        assertEquals("extension point [" + Pricing.class.getName() + "] with name [twice] already registered", e.getMessage());

        assertEquals(List.of(), context.listAllBusiness(), "a refused business is not in the registry");
        assertEquals(versionBefore, context.registryVersion(), "and the registry is what it was");
        SessionException none = assertThrows(SessionException.class, () -> context.initSession(new Req("anybody")));
        assertEquals("no business matched", none.getMessage());
    }

    @Test
    public void testRefusedAbilityLeavesNothingBehind() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        String versionBefore = context.registryVersion();

        RegisterDuplicateException e = assertThrows(RegisterDuplicateException.class, () -> context.registerAbility(
                new ShopAbility("twice", r -> true, Pricing.class, Pricing.class)));
        assertEquals("extension point [" + Pricing.class.getName() + "] with name [twice] already registered", e.getMessage());

        assertEquals(List.of(), context.listAllAbility(), "a refused ability is not in the registry");
        assertEquals(versionBefore, context.registryVersion(), "and the registry is what it was");
    }

    @Test
    public void testRefusedBusinessDoesNotTakePartInResolvingRequests() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.registerBusiness(new ShopBusiness("retail", r -> true, 0, List.of(Pricing.class), List.of()));

        // refused for having no code, after it had been stored
        RegisterParamException e = assertThrows(RegisterParamException.class, () -> context.registerBusiness(
                new ShopBusiness(null, r -> true, 3, List.of(Pricing.class), List.of())));
        assertEquals("instance code should not be null", e.getMessage());

        // used to be: "multiple business found, matched business codes: [retail, null]", for every request
        context.initSession(new Req("anybody"));
        try {
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRefusedBusinessCanBeRegisteredAgainOnceFixed() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        assertThrows(RegisterDuplicateException.class, () -> context.registerBusiness(
                new ShopBusiness("twice", r -> true, 5, List.of(Pricing.class, Pricing.class), List.of())));

        // the code is free again: not "business with code [twice] already register"
        context.registerBusiness(new ShopBusiness("twice", r -> true, 5, List.of(Pricing.class), List.of()));
        context.initSession(new Req("anybody"));
        try {
            assertEquals("twice pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }
}
