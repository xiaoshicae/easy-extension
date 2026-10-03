package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * "Nobody matched" and "several matched" used to share one strict/non-strict switch. They are two policies now:
 * the four combinations, and the old boolean constructor as shorthand for the two that used to exist.
 */
public class ResolutionPolicyTest {

    private static final String DEFAULT_CODE = "system.extension.point.default.implementation";

    /** Two businesses that both match a request called "both", and one that matches "only-a". */
    private static DefaultExtensionContext<Req> shop(UnknownBusinessPolicy unknown, MultiMatchPolicy multi) throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, unknown, multi, List.of());
        return populate(context);
    }

    private static DefaultExtensionContext<Req> populate(DefaultExtensionContext<Req> context) throws Exception {
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(new ShopBusiness("a", r -> r.name.equals("only-a") || r.name.equals("both"), 0, List.of(Pricing.class), List.of()));
        context.registerBusiness(new ShopBusiness("b", r -> r.name.equals("both"), 1, List.of(Pricing.class), List.of()));
        return context;
    }

    private static String priceOf(DefaultExtensionContext<Req> context, String request) throws Exception {
        context.initSession(new Req(request));
        try {
            return context.invoke(Pricing.class, Pricing::price);
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testRejectBoth() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT);

        assertEquals("a pricing", priceOf(context, "only-a"));

        SessionException none = assertThrows(SessionException.class, () -> context.initSession(new Req("nobody")));
        assertEquals("no business matched", none.getMessage());

        SessionException several = assertThrows(SessionException.class, () -> context.initSession(new Req("both")));
        assertEquals("multiple business found, matched business codes: [a, b]", several.getMessage());
    }

    @Test
    public void testUnknownBusinessMayFallBackToDefaultWhileSeveralStillFail() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.REJECT);

        assertEquals("default pricing", priceOf(context, "nobody"));

        SessionException several = assertThrows(SessionException.class, () -> context.initSession(new Req("both")));
        assertEquals("multiple business found, matched business codes: [a, b]", several.getMessage());
    }

    @Test
    public void testSeveralMayBeSelectedWhileUnknownStillFails() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.REJECT, MultiMatchPolicy.SELECT);

        assertEquals("a pricing", priceOf(context, "both"), "first registered wins by default");

        SessionException none = assertThrows(SessionException.class, () -> context.initSession(new Req("nobody")));
        assertEquals("no business matched", none.getMessage());
    }

    @Test
    public void testLenientOnBothCounts() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT);

        assertEquals("default pricing", priceOf(context, "nobody"));
        assertEquals("a pricing", priceOf(context, "both"));
    }

    @Test
    public void testBooleanConstructorIsShorthandForTheTwoClassicCombinations() throws Exception {
        DefaultExtensionContext<Req> strict = populate(new DefaultExtensionContext<>(false, true));
        SessionException none = assertThrows(SessionException.class, () -> strict.initSession(new Req("nobody")));
        assertEquals("no business matched", none.getMessage());
        SessionException several = assertThrows(SessionException.class, () -> strict.initSession(new Req("both")));
        assertEquals("multiple business found, matched business codes: [a, b]", several.getMessage());

        DefaultExtensionContext<Req> lenient = populate(new DefaultExtensionContext<>(false, false));
        assertEquals("default pricing", priceOf(lenient, "nobody"));
        assertEquals("a pricing", priceOf(lenient, "both"));
    }

    @Test
    public void testBusinessMatchOrderDecidesWhenSeveralMatch() throws Exception {
        DefaultExtensionContext<Req> context = populate(new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.SELECT, List.of("b", "a")));

        assertEquals("b pricing", priceOf(context, "both"));
    }

    @Test
    public void testSelectorReturningNothingFallsBackToTheDefaults() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT);
        context.setBusinessMatchSelector((List<IBusiness<Req>> matched, Req param) -> null);

        assertEquals("default pricing", priceOf(context, "both"));
    }

    @Test
    public void testSeveralMatchesAreWarnedAboutOncePerCombination() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        // a request matches every business whose code its name contains
        context.registerBusiness(new ShopBusiness("a", r -> r.name.contains("a"), 0, List.of(Pricing.class), List.of()));
        context.registerBusiness(new ShopBusiness("b", r -> r.name.contains("b"), 1, List.of(Pricing.class), List.of()));
        context.registerBusiness(new ShopBusiness("c", r -> r.name.contains("c"), 2, List.of(Pricing.class), List.of()));

        assertEquals(0, context.multiMatchWarningCount());

        for (int i = 0; i < 50; i++) {
            priceOf(context, "ab");
        }
        assertEquals(1, context.multiMatchWarningCount(), "the same combination is reported once, not once per request");

        priceOf(context, "a");
        assertEquals(1, context.multiMatchWarningCount(), "a single match is nothing to warn about");

        priceOf(context, "bc");
        assertEquals(2, context.multiMatchWarningCount(), "another combination is reported on its own");

        priceOf(context, "ba");
        assertEquals(2, context.multiMatchWarningCount(), "same businesses as \"ab\": same combination");
    }

    @Test
    public void testSelectionByCustomSelectorIsTheApplicationsOwnRuleAndDoesNotWarn() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.REJECT, MultiMatchPolicy.SELECT);
        context.setBusinessMatchSelector((List<IBusiness<Req>> matched, Req param) -> matched.get(matched.size() - 1));

        assertEquals("b pricing", priceOf(context, "both"));
        assertEquals(0, context.multiMatchWarningCount());
    }

    @Test
    public void testConfiguredBusinessMatchOrderDoesNotWarn() throws Exception {
        DefaultExtensionContext<Req> context = populate(new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.SELECT, List.of("b", "a")));

        assertEquals("b pricing", priceOf(context, "both"));
        assertEquals(0, context.multiMatchWarningCount(), "which one wins was said explicitly");
    }

    @Test
    public void testSelectionByRegistrationOrderWarns() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.REJECT, MultiMatchPolicy.SELECT);

        assertEquals("a pricing", priceOf(context, "both"));
        assertEquals(1, context.multiMatchWarningCount(), "nothing but the order of registration decided");
    }

    @Test
    public void testRejectionInANamedScopeSaysWhichScope() throws Exception {
        DefaultExtensionContext<Req> context = shop(UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT);

        SessionException e = assertThrows(SessionException.class, () -> context.initSession("tenant-x", new Req("nobody")));
        assertEquals("scope [tenant-x], no business matched", e.getMessage());
    }
}
