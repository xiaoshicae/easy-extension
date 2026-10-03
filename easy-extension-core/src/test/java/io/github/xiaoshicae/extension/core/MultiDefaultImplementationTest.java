package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.core.ShopFixtures.AllDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.CustomDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.CustomPricingDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.EmptyDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.PricingDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.Shipping;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShippingDefault;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Default implementations used to be one object that had to implement every extension point. They can now be split
 * up, each answering for the extension points it implements, and an extension point can be declared mandatory to
 * say it has no default at all.
 */
public class MultiDefaultImplementationTest {

    private static final String DEFAULT_CODE = "system.extension.point.default.implementation";

    private static DefaultExtensionContext<Req> newShop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        return ShopFixtures.emptyShop(context);
    }

    @Test
    public void testDefaultsAreSplitByExtensionPoint() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new ShippingDefault());
        context.registerBusiness(ShopBusiness.named("retail", Pricing.class, Tax.class));
        context.validateRegistration();

        context.initSession(new Req("retail"));
        try {
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
            assertEquals("default shipping", context.invoke(Shipping.class, Shipping::ship), "served by the shipping default");
            assertEquals("retail tax", context.invoke(Tax.class, Tax::tax));
            assertEquals(List.of("retail", DEFAULT_CODE), context.currentChain().codes(), "the defaults share one place in the chain");
        } finally {
            context.removeSession();
        }

        context.initSession(new Req("somebody else"));
        try {
            assertEquals("default pricing", context.invoke(Pricing.class, Pricing::price));
            assertEquals("default shipping", context.invoke(Shipping.class, Shipping::ship));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testMandatoryExtensionPointNobodyImplementsFailsWithAnExplicitMessage() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new AllDefaults());
        context.validateRegistration();

        context.initSession(new Req("nobody"));
        try {
            InvokeException e = assertThrows(InvokeException.class, () -> context.invoke(Tax.class, Tax::tax));
            assertEquals("invoke failed, Extension<" + Tax.class.getName() + "> not found, it is a mandatory extension point and none of ["
                    + DEFAULT_CODE + "] implements it", e.getMessage());

            // an extension point that is not mandatory keeps the plain message
            context.removeSession();
            DefaultExtensionContext<Req> noDefaultForShipping = newShop();
            noDefaultForShipping.addExtensionPointDefaultImplementation(new PricingDefault());
            noDefaultForShipping.initSession(new Req("nobody"));
            e = assertThrows(InvokeException.class, () -> noDefaultForShipping.invoke(Shipping.class, Shipping::ship));
            assertEquals("invoke failed, Extension<" + Shipping.class.getName() + "> not found", e.getMessage());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testDefaultsAreListedInRegistrationOrder() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        PricingDefault pricing = new PricingDefault();
        ShippingDefault shipping = new ShippingDefault();

        assertEquals(List.of(), context.listExtensionPointDefaultImplementations());
        context.addExtensionPointDefaultImplementation(pricing);
        context.addExtensionPointDefaultImplementation(shipping);

        assertEquals(List.of(pricing, shipping), context.listExtensionPointDefaultImplementations());
        assertSame(pricing, context.getExtensionPointDefaultImplementation(), "the one registered first");
    }

    @Test
    public void testExtensionPointWithADefaultAlreadyCannotGetAnother() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());

        RegisterException e = assertThrows(RegisterDuplicateException.class, () -> context.addExtensionPointDefaultImplementation(new AllDefaults()));
        assertEquals("extension point default implementation for [" + Pricing.class.getName() + "] already registered, ["
                + AllDefaults.class.getName() + "] cannot implement it again", e.getMessage());

        // the refused default left nothing behind: Shipping can still get its own
        context.addExtensionPointDefaultImplementation(new ShippingDefault());
        assertEquals(2, context.listExtensionPointDefaultImplementations().size());
    }

    @Test
    public void testAdditionalDefaultsAreChecked() throws Exception {
        DefaultExtensionContext<Req> context = newShop();

        RegisterException e = assertThrows(RegisterParamException.class, () -> context.addExtensionPointDefaultImplementation(null));
        assertEquals("extension point default implementation should not be null", e.getMessage());

        e = assertThrows(RegisterParamException.class, () -> context.addExtensionPointDefaultImplementation(new EmptyDefault()));
        assertEquals("extension point default implementation [" + EmptyDefault.class.getName() + "] should implement at least one extension point", e.getMessage());

        interface NotRegistered {
        }
        class DefaultForUnknown extends io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation<Req> implements NotRegistered {
            @Override
            public List<Class<?>> implementExtensionPoints() {
                return List.of(NotRegistered.class);
            }
        }
        e = assertThrows(RegisterException.class, () -> context.addExtensionPointDefaultImplementation(new DefaultForUnknown()));
        assertEquals("extension point [" + NotRegistered.class.getName() + "] not registered", e.getMessage());
    }

    @Test
    public void testDefaultsWithTheSameCodeMustAgreeOnPriority() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());   // code DEFAULT_CODE, priority Integer.MAX_VALUE

        RegisterException e = assertThrows(RegisterParamException.class,
                () -> context.addExtensionPointDefaultImplementation(new CustomDefault(DEFAULT_CODE, 5)));
        assertEquals("extension point default implementations with code [" + DEFAULT_CODE + "] should have the same priority, found ["
                + Integer.MAX_VALUE + "] and [5]", e.getMessage());
    }

    @Test
    public void testDefaultsWithDifferentCodesTakeASeparatePlaceEachInTheChain() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new CustomDefault("shipping.fallback", 100));
        context.initSession(new Req("nobody"));
        try {
            ResolvedChain chain = context.currentChain();
            assertEquals(List.of("shipping.fallback", DEFAULT_CODE), chain.codes(), "ordered by priority");
            assertEquals("shipping.fallback shipping", context.invoke(Shipping.class, Shipping::ship));
            assertEquals("default pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testDefaultsWithDifferentCodesCannotShareAPriority() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new CustomDefault("shipping.fallback", 100));

        // a chain holds one entry per priority: registering this would make every session fail to resolve
        RegisterException e = assertThrows(RegisterParamException.class,
                () -> context.addExtensionPointDefaultImplementation(new CustomPricingDefault("pricing.fallback", 100)));
        assertEquals("extension point default implementations with different codes should have different priorities, "
                + "[shipping.fallback] and [pricing.fallback] both have priority [100]", e.getMessage());

        // the refused default left nothing behind
        assertEquals(1, context.listExtensionPointDefaultImplementations().size());
        context.addExtensionPointDefaultImplementation(new CustomPricingDefault("pricing.fallback", 200));
        context.initSession(new Req("nobody"));
        try {
            assertEquals(List.of("shipping.fallback", "pricing.fallback"), context.currentChain().codes());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testDefaultsCannotTakeThePriorityOfTheSharedDefaultCodeEither() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new ShippingDefault());   // DEFAULT_CODE, Integer.MAX_VALUE

        RegisterException e = assertThrows(RegisterParamException.class,
                () -> context.addExtensionPointDefaultImplementation(new CustomPricingDefault("pricing.fallback", Integer.MAX_VALUE)));
        assertEquals("extension point default implementations with different codes should have different priorities, ["
                + DEFAULT_CODE + "] and [pricing.fallback] both have priority [" + Integer.MAX_VALUE + "]", e.getMessage());
    }

    @Test
    public void testTraceAndExplanationKnowEveryDefault() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());
        context.addExtensionPointDefaultImplementation(new CustomDefault("shipping.fallback", 100));
        context.initSession(new Req("nobody"));
        try {
            ResolveTrace trace = context.getLastResolveTrace();

            assertEquals(List.of("shipping.fallback", DEFAULT_CODE),
                    trace.getResolutionChain().stream().map(ResolutionEntry::code).toList());
            assertEquals(DEFAULT_CODE, trace.getDefaultImplCode(), "the single-valued getters report the default registered first");
            assertEquals(Integer.valueOf(Integer.MAX_VALUE), trace.getDefaultImplPriority());
            assertTrue(trace.toString().contains("default=" + DEFAULT_CODE + "(priority=" + Integer.MAX_VALUE + "), shipping.fallback(priority=100), "),
                    "listed in registration order: " + trace);
            assertEquals("shipping.fallback", context.explain(Shipping.class).selected().code());
            assertEquals(DEFAULT_CODE, context.explain(Pricing.class).selected().code());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testSingleDefaultRegistrationKeepsItsStrictRules() throws Exception {
        DefaultExtensionContext<Req> context = newShop();

        RegisterException e = assertThrows(RegisterParamException.class, () -> context.registerExtensionPointDefaultImplementation(new PricingDefault()));
        assertEquals("extension point default implementation should implement all extension point, but in fact, it has not implement ["
                + Shipping.class.getName() + ", " + Tax.class.getName() + "]", e.getMessage());

        context.addExtensionPointDefaultImplementation(new PricingDefault());
        e = assertThrows(RegisterDuplicateException.class, () -> context.registerExtensionPointDefaultImplementation(new AllDefaults()));
        assertEquals("extension point default implementation already registered", e.getMessage());
    }

    @Test
    public void testValidateRegistrationWithoutAnyDefault() throws Exception {
        DefaultExtensionContext<Req> context = newShop();

        RegisterException e = assertThrows(RegisterParamException.class, context::validateRegistration);
        assertEquals("extension point default implementation not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist", e.getMessage());
    }

    @Test
    public void testValidateRegistrationNamesTheExtensionPointsWithoutDefault() throws Exception {
        DefaultExtensionContext<Req> context = newShop();
        context.addExtensionPointDefaultImplementation(new PricingDefault());

        RegisterException e = assertThrows(RegisterParamException.class, context::validateRegistration);
        assertEquals("extension point default implementation should implement all extension point, but in fact, it has not implement ["
                + Shipping.class.getName() + "] (an extension point without a sensible default can be marked @ExtensionPoint(mandatory = true))", e.getMessage());

        context.addExtensionPointDefaultImplementation(new ShippingDefault());
        context.validateRegistration();   // Tax is mandatory, nothing else is missing
    }

    @Test
    public void testNothingToValidateWithoutExtensionPoints() throws Exception {
        new DefaultExtensionContext<Req>().validateRegistration();
    }

    @Test
    public void testHelperRegistersSplitDefaultsAndValidates() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>();
        io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterHelper<Req> helper =
                new io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterHelper<>(context);
        helper.addExtensionPointClasses(Pricing.class, Shipping.class, Tax.class)
                .setMatcherParamClass(Req.class)
                .addExtensionPointDefaultImplementations(new PricingDefault(), new ShippingDefault())
                .addBusinesses(ShopBusiness.named("retail", Pricing.class, Tax.class));

        helper.doRegister();

        assertEquals(2, context.listExtensionPointDefaultImplementations().size());

        // without a default for Shipping the helper refuses the registry
        DefaultExtensionContext<Req> incomplete = new DefaultExtensionContext<>();
        io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterHelper<Req> incompleteHelper =
                new io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterHelper<>(incomplete);
        incompleteHelper.addExtensionPointClasses(Pricing.class, Shipping.class, Tax.class)
                .setMatcherParamClass(Req.class)
                .addExtensionPointDefaultImplementations(new PricingDefault());
        RegisterException e = assertThrows(RegisterParamException.class, incompleteHelper::doRegister);
        assertEquals("extension point default implementation should implement all extension point, but in fact, it has not implement ["
                + Shipping.class.getName() + "] (an extension point without a sensible default can be marked @ExtensionPoint(mandatory = true))", e.getMessage());
    }
}
