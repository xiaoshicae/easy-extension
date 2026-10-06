package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.definition.DefaultImplementationDefinition;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import io.github.xiaoshicae.extension.core.spi.OrderedCodeBusinessSelector;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ResolutionTest {

    @Test
    public void testBusinessComesFirstByDefault() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).build();

        Resolution resolution = context.resolve(Param.of("retail", "alipay", "fast"));
        assertEquals("retail-pay", resolution.first(Pay.class).pay());
        assertEquals(List.of("retail-pay", "ability-alipay", "default-pay"),
                resolution.all(Pay.class).stream().map(Pay::pay).toList());
        // the business does not implement Ship: the mounted ability answers, else the default implementation
        assertEquals("ability-fast-ship", resolution.first(Ship.class).ship());
        assertEquals("default-ship", context.resolve(Param.of("retail")).first(Ship.class).ship());
    }

    @Test
    public void testSelfPositionLetsAnAbilityOverrideTheBusiness() {
        ExtensionContext<Param> context = Fixtures.base().business(new FreshBusiness()).build();

        Resolution withAbility = context.resolve(Param.of("fresh", "alipay"));
        assertEquals("ability-alipay", withAbility.first(Pay.class).pay());
        assertEquals(List.of("ability-alipay", "fresh-pay", "default-pay"),
                withAbility.all(Pay.class).stream().map(Pay::pay).toList());

        // the ability does not take part in this request: the business answers again
        assertEquals("fresh-pay", context.resolve(Param.of("fresh")).first(Pay.class).pay());
    }

    @Test
    public void testTraceRecordsChainAndSkippedAbilities() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).build();

        ResolveTrace trace = context.resolve(Param.of("retail", "fast")).trace();
        assertEquals("biz.retail", trace.matchedBusinessCode());
        assertEquals(List.of(
                new ResolveTrace.ChainEntry("biz.retail", ResolveTrace.EntryType.BUSINESS, 0),
                new ResolveTrace.ChainEntry("ability.fast-ship", ResolveTrace.EntryType.ABILITY, 2)), trace.chain());
        assertEquals(List.of(new ResolveTrace.SkippedAbility("ability.alipay", 1, "ability.match() returned false")),
                trace.skippedAbilities());
    }

    @Test
    public void testExplainShowsWhoWouldAnswer() {
        ExtensionContext<Param> context = Fixtures.base().business(new FreshBusiness()).build();

        ExtensionExplanation<Pay> explanation = context.resolve(Param.of("fresh", "alipay")).explain(Pay.class);
        assertEquals(3, explanation.candidates().size());
        assertEquals("ability.alipay", explanation.selected().code());
        assertEquals(List.of("ability.alipay", "biz.fresh", DefaultPay.class.getName()),
                explanation.candidates().stream().map(ExtensionExplanation.Candidate::code).toList());

        // FreshBusiness is the only one implementing Ship here
        ExtensionExplanation<Ship> ship = context.resolve(Param.of("fresh", "alipay")).explain(Ship.class);
        assertEquals("biz.fresh", ship.selected().code());
        assertFalse(ship.candidates().get(0).implementsExtensionPoint());
    }

    @Test
    public void testDefaultImplementationAnswersWithoutBusinessWhenNotStrict() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).strict(false).build();

        Resolution resolution = context.resolve(Param.of("unknown", "alipay"));
        assertNull(resolution.trace().matchedBusinessCode());
        assertEquals("default-pay", resolution.first(Pay.class).pay());
    }

    @Test
    public void testStrictModeRejectsNoBusiness() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).build();

        ResolutionException e = assertThrows(ResolutionException.class, () -> context.resolve(Param.of("unknown")));
        assertEquals(Reason.NO_BUSINESS_MATCHED, e.reason());
        assertEquals("no business matched", e.getMessage());
    }

    @Test
    public void testStrictModeRejectsSeveralBusinesses() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).business(new SecondRetailBusiness()).build();

        ResolutionException e = assertThrows(ResolutionException.class, () -> context.resolve(Param.of("retail")));
        assertEquals(Reason.MULTIPLE_BUSINESSES_MATCHED, e.reason());
        assertEquals("multiple business found, matched business codes: [biz.retail, biz.retail.2]", e.getMessage());
    }

    @Test
    public void testSelectorSettlesSeveralMatchingBusinesses() {
        ExtensionContext<Param> byOrder = Fixtures.base().strict(false)
                .business(new RetailBusiness()).business(new SecondRetailBusiness())
                .businessSelector(new OrderedCodeBusinessSelector<>(List.of("biz.retail.2", "biz.retail"))).build();
        assertEquals("retail2-pay", byOrder.resolve(Param.of("retail")).first(Pay.class).pay());

        // unlisted codes: the first registered business wins
        ExtensionContext<Param> unlisted = Fixtures.base().strict(false)
                .business(new RetailBusiness()).business(new SecondRetailBusiness()).build();
        assertEquals("retail-pay", unlisted.resolve(Param.of("retail")).first(Pay.class).pay());

        ExtensionContext<Param> none = Fixtures.base().strict(false)
                .business(new RetailBusiness()).business(new SecondRetailBusiness())
                .businessSelector((codes, param) -> null).build();
        assertEquals("default-pay", none.resolve(Param.of("retail")).first(Pay.class).pay());

        ExtensionContext<Param> bogus = Fixtures.base().strict(false)
                .business(new RetailBusiness()).business(new SecondRetailBusiness())
                .businessSelector((codes, param) -> "biz.nope").build();
        ResolutionException e = assertThrows(ResolutionException.class, () -> bogus.resolve(Param.of("retail")));
        assertEquals(Reason.BUSINESS_NOT_FOUND, e.reason());
        assertEquals("business [biz.nope] chosen by BusinessSelector is not among the matched businesses [biz.retail, biz.retail.2]", e.getMessage());
    }

    @Test
    public void testBusinessResolverRoutesByCodeAndIgnoresMatchers() {
        // FreshBusiness.match would be false for tenant "x": the resolver decides alone
        ExtensionContext<Param> context = Fixtures.base().business(new FreshBusiness()).business(new RetailBusiness())
                .businessResolver(param -> "x".equals(param.tenant()) ? Optional.of("biz.fresh")
                        : "ghost".equals(param.tenant()) ? Optional.of("biz.ghost") : Optional.empty())
                .build();

        assertEquals("fresh-pay", context.resolve(Param.of("x")).first(Pay.class).pay());

        ResolutionException unknown = assertThrows(ResolutionException.class, () -> context.resolve(Param.of("ghost")));
        assertEquals(Reason.BUSINESS_NOT_FOUND, unknown.reason());
        assertEquals("business [biz.ghost] resolved by BusinessResolver is not registered", unknown.getMessage());

        // empty is "no business", it does not fall back to matching (the retail business would match "retail")
        ResolutionException empty = assertThrows(ResolutionException.class, () -> context.resolve(Param.of("retail")));
        assertEquals(Reason.NO_BUSINESS_MATCHED, empty.reason());
    }

    @Test
    public void testBusinessWithoutMatcherWorksWithAResolver() {
        ExtensionContext<Param> context = Fixtures.base()
                .business(BusinessDefinition.<Param>of("biz.manual", null, new ManualBusiness()))
                .businessResolver(param -> Optional.of("biz.manual")).build();

        assertEquals("manual-pay", context.resolve(Param.of("any")).first(Pay.class).pay());
    }

    public static class ManualBusiness implements Pay {
        @Override
        public String pay() {
            return "manual-pay";
        }
    }

    @Test
    public void testVoidExtensionPointGetsANoOpDefault() {
        ExtensionContext<Param> context = Fixtures.base().business(new AuditedBusiness()).business(new RetailBusiness()).build();

        List<String> audited = new ArrayList<>();
        context.resolve(Param.of("audited")).first(Audit.class).audit(audited);
        assertEquals(List.of("ability-risk"), audited);

        Resolution retail = context.resolve(Param.of("retail"));
        List<String> trail = new ArrayList<>();
        retail.first(Audit.class).audit(trail);
        assertEquals(List.of(), trail);
        assertEquals(1, retail.all(Audit.class).size());
        assertEquals("NoOpDefault<" + Audit.class.getName() + ">", retail.first(Audit.class).toString());
    }

    @Test
    public void testUnregisteredExtensionPoint() {
        Resolution resolution = Fixtures.base().business(new RetailBusiness()).build().resolve(Param.of("retail"));

        ResolutionException e = assertThrows(ResolutionException.class, () -> resolution.first(Runnable.class));
        assertEquals(Reason.EXTENSION_NOT_FOUND, e.reason());
        assertEquals("extension point [java.lang.Runnable] is not registered", e.getMessage());
    }

    @Test
    public void testInvokeHelpers() {
        Resolution resolution = Fixtures.base().business(new RetailBusiness()).build().resolve(Param.of("retail", "alipay"));

        assertEquals("RETAIL-PAY", resolution.invoke(Pay.class, e -> e.pay().toUpperCase()));
        assertEquals(List.of(10, 14, 11), resolution.invokeAll(Pay.class, e -> e.pay().length()));
        assertEquals(35, resolution.invokeReduce(Pay.class, e -> e.pay().length(), 0, Integer::sum));
    }

    @Test
    public void testUnknownBusinessFromAResolverIsNoBusinessWhenNotStrict() {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).strict(false)
                .businessResolver(param -> Optional.of("biz.not-onboarded")).build();

        Resolution resolution = context.resolve(Param.of("retail", "alipay"));
        assertNull(resolution.trace().matchedBusinessCode());
        assertEquals("default-pay", resolution.first(Pay.class).pay());
    }

    public static class SharedPay implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String pay() {
            return "shared-pay";
        }
    }

    @Test
    public void testAnObjectPlayingSeveralRolesIsListedOnce() {
        SharedPay shared = new SharedPay();
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().extensionPoint(Pay.class)
                .defaultImplementation(DefaultImplementationDefinition.of(shared))
                .ability(AbilityDefinition.of("ability.shared", shared, shared))
                .business(BusinessDefinition.<Param>of("biz.simple", param -> true, new RetailBusiness()).ability("ability.shared"))
                .build();

        assertEquals(List.of("retail-pay", "shared-pay"),
                context.resolve(Param.of("any")).all(Pay.class).stream().map(Pay::pay).toList());
    }
}
