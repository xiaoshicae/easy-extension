package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.catalog.AbilityInfo;
import io.github.xiaoshicae.extension.core.catalog.BusinessInfo;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.catalog.ExtensionPointInfo;
import io.github.xiaoshicae.extension.core.catalog.MountInfo;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.exception.RegistrationException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CatalogTest {

    @Test
    public void testCatalogDescribesWhatIsRegistered() {
        ExtensionCatalog catalog = Fixtures.base().ability(new InstallmentAbility())
                .business(new RetailBusiness()).business(new FreshBusiness()).build().catalog();

        assertEquals(List.of(Pay.class, Ship.class, Audit.class), catalog.extensionPoints().stream().map(ExtensionPointInfo::type).toList());
        assertEquals(1, catalog.extensionPoints().get(0).version());

        AbilityInfo installment = catalog.abilities().stream().filter(a -> a.code().equals("ability.installment")).findFirst().orElseThrow();
        assertEquals(InstallmentAbility.class, installment.implementationClass());
        assertEquals(List.of(Pay.class), installment.extensionPoints());
        assertEquals(List.of("ability.risk"), installment.requires());
        assertEquals(List.of(), installment.excludes());

        assertEquals(2, catalog.defaultImplementations().size());
        assertEquals(DefaultPay.class, catalog.defaultImplementations().get(0).implementationClass());
    }

    @Test
    public void testBusinessMountsIncludeTheBusinessItselfInPrecedenceOrder() {
        ExtensionCatalog catalog = Fixtures.base().business(new RetailBusiness()).business(new FreshBusiness()).build().catalog();

        BusinessInfo retail = catalog.businesses().get(0);
        // Self is not listed in abilities: the business comes first
        assertEquals(List.of(MountInfo.self(), MountInfo.ability("ability.alipay"), MountInfo.ability("ability.fast-ship")), retail.mounts());
        BusinessInfo fresh = catalog.businesses().get(1);
        // Self placed after the ability
        assertEquals(List.of(MountInfo.ability("ability.alipay"), MountInfo.self()), fresh.mounts());
        assertTrue(fresh.mounts().get(1).isSelf());
    }

    // ---- request parameter type

    public static abstract class GenericAbility<X> implements Matcher<X>, Pay {
        @Override
        public boolean match(X param) {
            return true;
        }

        @Override
        public String pay() {
            return "generic";
        }
    }

    @Ability(code = "ability.inherited-generic")
    public static class InheritedGenericAbility extends GenericAbility<Param> {
    }

    @Ability(code = "ability.string")
    public static class StringAbility implements Matcher<String>, Pay {
        @Override
        public boolean match(String param) {
            return true;
        }

        @Override
        public String pay() {
            return "string";
        }
    }

    public static class Plain implements Pay {
        @Override
        public String pay() {
            return "plain";
        }
    }

    private static ExtensionContextBuilder<Param> payOnly() {
        return ExtensionContext.<Param>builder().extensionPoint(Pay.class).defaultImplementation(new DefaultPay());
    }

    @Test
    public void testMatcherParamTypeIsDerivedFromTheMatchers() {
        assertEquals(Param.class, Fixtures.base().build().catalog().matcherParamType());
        // through a generic superclass
        assertEquals(Param.class, payOnly().ability(new InheritedGenericAbility()).build().catalog().matcherParamType());
    }

    @Test
    public void testMatcherParamTypeIsUnknownForLambdasAndCanBeGivenExplicitly() {
        ExtensionContextBuilder<Param> builder = payOnly().ability(AbilityDefinition.of("ability.lambda", param -> true, new Plain()));
        assertNull(builder.build().catalog().matcherParamType());
        assertEquals(Param.class, builder.matcherParamType(Param.class).build().catalog().matcherParamType());
        assertNull(payOnly().build().catalog().matcherParamType());
    }

    @Test
    public void testMatchersOfDifferentParameterTypesAreRejected() {
        ExtensionContextBuilder<Param> builder = payOnly().ability(new InheritedGenericAbility());
        @SuppressWarnings({"unchecked", "rawtypes"})
        ExtensionContextBuilder<Param> mixed = (ExtensionContextBuilder) builder.ability(AbilityDefinition.of("ability.string", (Matcher) new StringAbility(), new StringAbility()).implementationClass(StringAbility.class));

        RegistrationException e = assertThrows(RegistrationException.class, mixed::build);
        assertEquals("abilities and businesses match on different parameter types: " + List.of(Param.class, String.class)
                + ", they must share one (or set matcherParamType explicitly)", e.getMessage());
    }

    @Ability(code = "ability.object")
    public static class ObjectMatcherAbility implements Matcher<Object>, Pay {
        @Override
        public boolean match(Object param) {
            return true;
        }

        @Override
        public String pay() {
            return "object";
        }
    }

    public static class SimpleBusiness implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String pay() {
            return "simple";
        }
    }

    public static abstract class Bounded<X extends Param> implements Matcher<X>, Pay {
        @Override
        public boolean match(X param) {
            return true;
        }

        @Override
        public String pay() {
            return "bounded";
        }
    }

    @SuppressWarnings("rawtypes")
    @Ability(code = "ability.bounded")
    public static class RawBoundedAbility extends Bounded {
    }

    @Test
    public void testAGeneralMatcherAndAMoreSpecificOneAreCompatible() {
        // Matcher<Object> accepts anything, so the request parameter type is the more specific Param
        ExtensionCatalog catalog = payOnly().ability(new ObjectMatcherAbility())
                .business(BusinessDefinition.<Param>of("biz.simple", new SimpleBusiness(), new SimpleBusiness())).build().catalog();
        assertEquals(Param.class, catalog.matcherParamType());
    }

    @Test
    public void testTheConfiguredTypeMustBeAcceptedByEveryMatcher() {
        RegistrationException e = assertThrows(RegistrationException.class,
                () -> payOnly().ability(new StringAbility()).matcherParamType(Param.class).build());
        assertEquals("ability [ability.string] matches on [java.lang.String], which does not accept the configured matcher param type ["
                + Param.class.getName() + "]", e.getMessage());

        assertEquals(Param.class, payOnly().ability(new ObjectMatcherAbility()).matcherParamType(Param.class).build().catalog().matcherParamType());
    }

    @Test
    public void testAnUnboundTypeVariableFallsBackToItsBound() {
        assertEquals(Param.class, payOnly().ability(new RawBoundedAbility()).build().catalog().matcherParamType());
    }

    @Test
    public void testBusinessMatchersAreIgnoredWhenABusinessResolverRoutes() {
        // a business whose Matcher<String> would clash with the abilities' Matcher<Param> is irrelevant with a resolver
        ExtensionContextBuilder<Param> builder = payOnly().ability(new InheritedGenericAbility())
                .business(BusinessDefinition.<Param>of("biz.string", null, new StringBusiness()).implementationClass(StringBusiness.class));
        assertEquals(Param.class, builder.businessResolver(param -> java.util.Optional.of("biz.string")).build().catalog().matcherParamType());
    }

    public static class StringBusiness implements Matcher<String>, Pay {
        @Override
        public boolean match(String param) {
            return true;
        }

        @Override
        public String pay() {
            return "string";
        }
    }
}
