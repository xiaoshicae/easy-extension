package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.Self;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Everything wrong with the setup is reported by {@code build()}, never at request time.
 */
public class RegistrationTest {

    private static String buildError(ExtensionContextBuilder<Param> builder) {
        return assertThrows(RegistrationException.class, builder::build).getMessage();
    }

    // ---- extension points

    interface NotPublic {
    }

    public interface NotAnnotated {
    }

    @Test
    public void testExtensionPointMustBeAPublicAnnotatedInterface() {
        assertEquals("extension point [java.lang.String] should be a public interface",
                buildError(ExtensionContext.<Param>builder().extensionPoint(String.class)));
        assertEquals("extension point [" + NotPublic.class.getName() + "] should be a public interface",
                buildError(ExtensionContext.<Param>builder().extensionPoint(NotPublic.class)));
        assertEquals("extension point [" + NotAnnotated.class.getName() + "] should be annotated with @ExtensionPoint",
                buildError(ExtensionContext.<Param>builder().extensionPoint(NotAnnotated.class)));
    }

    // ---- default implementations

    @Test
    public void testExtensionPointWithReturnValuesNeedsADefaultImplementation() {
        assertEquals("extension point [" + Ship.class.getName() + "] has no default implementation: "
                        + "add a @DefaultImplementation class for it "
                        + "(only an extension point whose methods all return void gets a no-op default automatically)",
                buildError(ExtensionContext.<Param>builder().extensionPoint(Pay.class, Ship.class).defaultImplementation(new DefaultPay())));
    }

    @DefaultImplementation
    public static class AnotherDefaultPay implements Pay {
        @Override
        public String pay() {
            return "another";
        }
    }

    @DefaultImplementation
    public static class DefaultPayAndShip implements Pay, Ship {
        @Override
        public String pay() {
            return "pay";
        }

        @Override
        public String ship() {
            return "ship";
        }
    }

    @Test
    public void testOneDefaultImplementationMayBackSeveralExtensionPointsButNotTwoForOne() {
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().extensionPoint(Pay.class, Ship.class, Audit.class)
                .defaultImplementation(new DefaultPayAndShip()).strict(false).build();
        Resolution resolution = context.resolve(Param.of("any"));
        assertEquals("pay", resolution.first(Pay.class).pay());
        assertEquals("ship", resolution.first(Ship.class).ship());

        assertEquals("extension point [" + Pay.class.getName() + "] has more than one default implementation: ["
                        + DefaultPay.class.getName() + "] and [" + AnotherDefaultPay.class.getName() + "]",
                buildError(ExtensionContext.<Param>builder().extensionPoint(Pay.class, Ship.class)
                        .defaultImplementation(new DefaultPay()).defaultImplementation(new AnotherDefaultPay())
                        .defaultImplementation(new DefaultShip())));
    }

    public static class UnmarkedDefault implements Pay {
        @Override
        public String pay() {
            return "x";
        }
    }

    @Test
    public void testAnnotatedObjectsMustCarryTheirAnnotation() {
        assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().defaultImplementation(new UnmarkedDefault()),
                "default implementation [" + UnmarkedDefault.class.getName() + "] must be annotated with @DefaultImplementation");
        assertEquals("ability [" + UnmarkedDefault.class.getName() + "] must be annotated with @Ability",
                assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().ability(new UnmarkedDefault())).getMessage());
        assertEquals("business [" + UnmarkedDefault.class.getName() + "] must be annotated with @Business",
                assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().business(new UnmarkedDefault())).getMessage());
    }

    // ---- abilities and businesses

    @Ability(code = "ability.no-matcher")
    public static class NoMatcherAbility implements Pay {
        @Override
        public String pay() {
            return "x";
        }
    }

    @Business(code = "biz.no-matcher")
    public static class NoMatcherBusiness implements Pay {
        @Override
        public String pay() {
            return "x";
        }
    }

    @Test
    public void testAbilitiesAndBusinessesNeedAMatcher() {
        assertEquals("ability [ability.no-matcher] should implement Matcher",
                buildError(Fixtures.base().ability(new NoMatcherAbility())));
        assertEquals("business [biz.no-matcher] should implement Matcher",
                buildError(Fixtures.base().business(new NoMatcherBusiness())));
        // a definition without a matcher is no longer allowed: every business identifies its own requests
        assertEquals("business [biz.manual] should implement Matcher",
                buildError(Fixtures.base().business(BusinessDefinition.<Fixtures.Param>of("biz.manual", null, new NoMatcherBusiness()))));
    }

    @Test
    public void testCodesAreUnique() {
        assertEquals("ability [ability.alipay] already registered", buildError(Fixtures.base().ability(new AlipayAbility())));
        assertEquals("business [biz.retail] already registered",
                buildError(Fixtures.base().business(new RetailBusiness()).business(new RetailBusiness())));
        // an ability and a business share one namespace: traces and the admin console show bare codes
        assertEquals("code [ability.alipay] is used by both an ability and a business",
                buildError(Fixtures.base().business(BusinessDefinition.<Param>of("ability.alipay", new RetailBusiness(), new RetailBusiness()))));
    }

    // ---- abilities

    @Business(code = "biz.ghost-user", abilities = SlowShipAbility.class)
    public static class UsesUnregisteredAbility implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String pay() {
            return "x";
        }
    }

    @Business(code = "biz.double-user", abilities = {AlipayAbility.class, AlipayAbility.class})
    public static class UsesAbilityTwice extends UsesUnregisteredAbility {
    }

    @Business(code = "biz.double-self", abilities = {Self.class, AlipayAbility.class, Self.class})
    public static class UsesSelfTwice extends UsesUnregisteredAbility {
    }

    @Business(code = "biz.string-user", abilities = String.class)
    public static class UsesANonAbility extends UsesUnregisteredAbility {
    }

    @Test
    public void testUsesMustReferenceRegisteredAbilitiesOnce() {
        assertEquals("business [biz.ghost-user] uses ability [ability.slow-ship] which is not registered",
                buildError(Fixtures.base().business(new UsesUnregisteredAbility())));
        assertEquals("business [biz.double-user] uses ability [ability.alipay] more than once",
                buildError(Fixtures.base().business(new UsesAbilityTwice())));
        assertEquals("business [biz.double-self] places itself (Self) more than once in abilities",
                buildError(Fixtures.base().business(new UsesSelfTwice())));
        assertEquals("[java.lang.String] is referenced as an ability but is not annotated with @Ability",
                assertThrows(RegistrationException.class, () -> Fixtures.base().business(new UsesANonAbility())).getMessage());
    }

    // ---- requires / excludes

    @Business(code = "biz.installment", abilities = InstallmentAbility.class)
    public static class InstallmentWithoutRisk extends UsesUnregisteredAbility {
    }

    @Business(code = "biz.installment-risk", abilities = {InstallmentAbility.class, RiskAbility.class})
    public static class InstallmentWithRisk extends UsesUnregisteredAbility {
    }

    @Business(code = "biz.slow-fast", abilities = {SlowShipAbility.class, FastShipAbility.class})
    public static class SlowAndFast extends UsesUnregisteredAbility {
    }

    @Test
    public void testRequiresAndExcludesMeanMountedTogether() {
        ExtensionContextBuilder<Param> base = Fixtures.base().ability(new InstallmentAbility()).ability(new SlowShipAbility());

        assertEquals("business [biz.installment] mounts ability [ability.installment] which requires ability [ability.risk], "
                        + "but [ability.risk] is not mounted",
                buildError(base.business(new InstallmentWithoutRisk())));

        // order does not matter, only that both are mounted
        ExtensionContext<Param> ok = Fixtures.base().ability(new InstallmentAbility()).business(new InstallmentWithRisk()).build();
        assertEquals(java.util.List.of("x", "ability-installment", "default-pay"),
                ok.resolve(Param.of("any", "installment")).all(Pay.class).stream().map(Pay::pay).toList());

        assertEquals("business [biz.slow-fast] mounts ability [ability.slow-ship] which excludes ability [ability.fast-ship], "
                        + "but both are mounted",
                buildError(Fixtures.base().ability(new SlowShipAbility()).business(new SlowAndFast())));
    }

    @Test
    public void testRequiresMustReferenceARegisteredAbility() {
        // InstallmentAbility requires RiskAbility, which is not registered here
        assertEquals("ability [ability.installment] requires ability [ability.risk] which is not registered",
                buildError(ExtensionContext.<Param>builder().extensionPoint(Pay.class).defaultImplementation(new DefaultPay())
                        .ability(new InstallmentAbility())));
    }

    // ---- derivation

    public static class Unrelated implements Matcher<Param> {
        @Override
        public boolean match(Param param) {
            return true;
        }
    }

    @Business(code = "biz.identity-only")
    public static class IdentityOnlyBusiness implements Matcher<Param> {
        @Override
        public boolean match(Param param) {
            return "identity".equals(param.tenant());
        }
    }

    @Test
    public void testBusinessMayImplementNoExtensionPointAndFallsThroughToTheDefaults() {
        ExtensionContext<Param> context = Fixtures.base().business(new IdentityOnlyBusiness()).build();

        assertEquals(List.of(), context.catalog().businesses().get(0).extensionPoints());
        Resolution resolution = context.resolve(Param.of("identity"));
        assertEquals("default-pay", resolution.first(Pay.class).pay());
        assertEquals("default-ship", resolution.first(Ship.class).ship());
    }

    @Test
    public void testProvidersMustImplementARegisteredExtensionPoint() {
        assertEquals("ability [ability.unrelated] does not implement any extension point",
                buildError(Fixtures.base().ability(AbilityDefinition.of("ability.unrelated", new Unrelated(), new Unrelated()))));

        // RiskAbility implements Audit, which is not registered in this builder
        assertEquals("extension point [" + Audit.class.getName() + "] implemented by ability [ability.risk] is not registered: "
                        + "register it, or add its package to the scan",
                buildError(ExtensionContext.<Param>builder().extensionPoint(Pay.class, Ship.class)
                        .defaultImplementation(new DefaultPay()).defaultImplementation(new DefaultShip()).ability(new RiskAbility())));
    }

    // ---- constraints between abilities

    @Ability(code = "ability.self-requiring", requires = SelfRequiring.class)
    public static class SelfRequiring implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String pay() {
            return "x";
        }
    }

    @Ability(code = "ability.both", requires = RiskAbility.class, excludes = RiskAbility.class)
    public static class RequiresAndExcludesTheSame extends SelfRequiring {
    }

    @Ability(code = "ability.needy", requires = Hater.class)
    public static class Needy extends SelfRequiring {
    }

    @Ability(code = "ability.hater", excludes = Needy.class)
    public static class Hater extends SelfRequiring {
    }

    @Test
    public void testContradictoryConstraintsAreRejectedEvenIfNothingMountsThem() {
        assertEquals("ability [ability.self-requiring] cannot require or exclude itself",
                buildError(Fixtures.base().ability(new SelfRequiring())));
        assertEquals("ability [ability.both] both requires and excludes ability [ability.risk]",
                buildError(Fixtures.base().ability(new RequiresAndExcludesTheSame())));
        assertEquals("ability [ability.needy] requires ability [ability.hater], which excludes it",
                buildError(Fixtures.base().ability(new Needy()).ability(new Hater())));
    }

    // ---- implementation vs. its class

    public static class RetailBusinessSubclass extends RetailBusiness {
    }

    @Test
    public void testImplementationMayBeASubclassOfTheUserClass() {
        // what a CGLIB proxy looks like: an instance of a subclass, the annotations live on the user class
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusinessSubclass(), RetailBusiness.class).build();

        assertEquals(RetailBusiness.class, context.catalog().businesses().get(0).implementationClass());
        assertEquals("retail-pay", context.resolve(Param.of("retail")).first(Pay.class).pay());
    }

    @Test
    public void testJdkProxyOfTheUserClassIsAccepted() {
        // Spring with proxy-target-class=false hands over a JDK proxy together with the target class
        Object jdkProxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Pay.class, Matcher.class},
                (proxy, method, args) -> method.getName().equals("match") ? Boolean.TRUE : "proxied-pay");

        ExtensionContext<Param> context = Fixtures.base().business(jdkProxy, RetailBusiness.class).build();

        assertEquals(RetailBusiness.class, context.catalog().businesses().get(0).implementationClass());
        assertEquals("proxied-pay", context.resolve(Param.of("anyone")).first(Pay.class).pay());
    }

    @Test
    public void testImplementationMustProvideTheExtensionPointsOfItsUserClass() {
        Object notAPay = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Matcher.class},
                (proxy, method, args) -> Boolean.TRUE);

        RegistrationException e = assertThrows(RegistrationException.class,
                () -> Fixtures.base().business(notAPay, RetailBusiness.class).build());
        assertEquals("business [biz.retail] is not an instance of extension point [" + Pay.class.getName() + "]", e.getMessage());
    }

    @Test
    public void testImplementationsMayNotBeNull() {
        assertEquals("implementation should not be null",
                assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().defaultImplementation((Object) null)).getMessage());
        assertEquals("implementation should not be null",
                assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().ability((Object) null)).getMessage());
        assertEquals("implementation should not be null",
                assertThrows(RegistrationException.class, () -> ExtensionContext.<Param>builder().business((Object) null)).getMessage());
    }

    @Test
    public void testChangingADefinitionAfterBuildDoesNotAffectTheContext() {
        AbilityDefinition<Param> definition = AbilityDefinition.of("ability.alipay", new AlipayAbility(), new AlipayAbility());
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().extensionPoint(Pay.class)
                .defaultImplementation(new DefaultPay()).ability(definition).build();

        definition.requires("ability.risk");
        assertEquals(java.util.List.of(), context.catalog().abilities().get(0).requires());
    }
}
