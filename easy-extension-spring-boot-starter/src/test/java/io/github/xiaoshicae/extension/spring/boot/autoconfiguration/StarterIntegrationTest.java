package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.catalog.AbilityInfo;
import io.github.xiaoshicae.extension.core.catalog.BusinessInfo;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.catalog.ExtensionPointInfo;
import io.github.xiaoshicae.extension.core.definition.AbilityDefinition;
import io.github.xiaoshicae.extension.core.definition.BusinessDefinition;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.DeferredExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.*;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.DomainConfig;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The starter end to end: what is scanned, how the context is assembled and when, and how injection behaves.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class StarterIntegrationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class))
            .withUserConfiguration(DomainConfig.class);

    private static ExtensionContext<Param> extensionContext(org.springframework.context.ApplicationContext context) {
        return context.getBean(ExtensionContext.class);
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    // ---- assembly

    @Test
    public void testContextIsBuiltFromScannedComponents() {
        FastShipAbility.INSTANCES.set(0);
        runner.run(context -> {
            ExtensionCatalog catalog = extensionContext(context).catalog();

            assertEquals(Set.of(Pay.class, Ship.class), catalog.extensionPoints().stream().map(ExtensionPointInfo::type).collect(Collectors.toSet()));
            assertEquals(Set.of("ability.fast-ship", "ability.composed"), catalog.abilities().stream().map(AbilityInfo::code).collect(Collectors.toSet()));
            assertEquals(Set.of("biz.retail", "biz.composed"), catalog.businesses().stream().map(BusinessInfo::code).collect(Collectors.toSet()));
            assertEquals(2, catalog.defaultImplementations().size());
            assertEquals(Param.class, catalog.matcherParamType());
            // a scanned provider is one Spring bean: the framework calls the bean the container owns
            assertEquals(1, FastShipAbility.INSTANCES.get());
            assertSame(context.getBean(FastShipAbility.class), extensionContext(context).resolve(new Param("retail")).first(Ship.class));
        });
    }

    @Test
    public void testNothingScannedMeansAnEmptyContext() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class)).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(extensionContext(context).catalog().extensionPoints().isEmpty());
        });
    }

    @Configuration
    @ExtensionScan(basePackages = "io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture")
    static class OverlappingScan {
    }

    @Test
    public void testOverlappingScanPackagesRegisterEverythingOnce() {
        runner.withUserConfiguration(OverlappingScan.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(2, extensionContext(context).catalog().extensionPoints().size());
        });
    }

    @Test
    public void testGenericInjectionOfTheContext() {
        runner.withUserConfiguration(TypedContextHolder.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertSame(extensionContext(context), context.getBean(TypedContextHolder.class).context);
        });
    }

    public static class TypedContextHolder {
        @Autowired
        ExtensionContext<Param> context;
    }

    // ---- injection

    @Test
    public void testInjectedProxiesFollowTheBinding() {
        runner.run(context -> {
            OrderService orderService = context.getBean(OrderService.class);
            ExtensionContext<Param> extensionContext = extensionContext(context);

            try (Binding ignored = extensionContext.bind(new Param("retail"))) {
                assertEquals("fast-ship", orderService.ship.ship());
                assertEquals(List.of("retail-pay", "default-pay"), orderService.allPay.stream().map(Pay::pay).toList());
            }
            try (Binding ignored = extensionContext.bind(new Param("composed"))) {
                assertEquals("composed-ship", orderService.ship.ship());
            }
        });
    }

    @Test
    public void testAbilitiesAndBusinessesMayUseExtensionPointsThemselves() {
        // ComposedAbility has an @ExtensionInject field and a dependency on a bean that has one as well.
        // In 3.x this failed at startup with a circular reference.
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            try (Binding ignored = extensionContext(context).bind(new Param("composed"))) {
                assertEquals("composed(composed-ship,composed-ship)", extensionContext(context).first(Pay.class).pay());
            }
        });
    }

    // ---- when the context is built

    @Configuration
    static class LazyEverything {
        // what spring.main.lazy-initialization=true does: every definition without an explicit setting becomes lazy
        @Bean
        static BeanFactoryPostProcessor makeEverythingLazy() {
            return beanFactory -> {
                for (String name : beanFactory.getBeanDefinitionNames()) {
                    if (beanFactory.getBeanDefinition(name) instanceof AbstractBeanDefinition definition && definition.getLazyInit() == null) {
                        definition.setLazyInit(true);
                    }
                }
            };
        }
    }

    @Test
    public void testLazyInitializationStillBuildsTheContext() {
        runner.withUserConfiguration(LazyEverything.class).run(context -> {
            assertNull(context.getStartupFailure());
            try (Binding ignored = extensionContext(context).bind(new Param("retail"))) {
                assertEquals("retail-pay", extensionContext(context).first(Pay.class).pay());
            }
        });
    }

    public static class EarlyUser implements InitializingBean {
        @Autowired
        ExtensionContext<Param> context;

        @Override
        public void afterPropertiesSet() {
            context.resolve(new Param("retail"));
        }
    }

    @Test
    public void testUsingTheContextWhileBeansAreBeingCreatedFailsClearly() {
        runner.withUserConfiguration(EarlyUser.class).run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCause(context.getStartupFailure()).getMessage().startsWith("the ExtensionContext is not ready yet"),
                    rootCause(context.getStartupFailure()).getMessage());
        });
    }

    // ---- proxied providers

    @Configuration
    static class ProxyAbilities {
        // what AOP does to a bean: replaces it with a CGLIB subclass that delegates to the original
        @Bean
        static BeanPostProcessor proxyAbilities() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean.getClass().isAnnotationPresent(Ability.class)) {
                        ProxyFactory factory = new ProxyFactory(bean);
                        factory.setProxyTargetClass(true);
                        return factory.getProxy();
                    }
                    return bean;
                }
            };
        }
    }

    @Test
    public void testSpringProxiesOfProvidersAreRegisteredAsTheirUserClass() {
        runner.withUserConfiguration(ProxyAbilities.class).run(context -> {
            assertNull(context.getStartupFailure());
            ExtensionContext<Param> extensionContext = extensionContext(context);
            AbilityInfo info = extensionContext.catalog().abilities().stream().filter(a -> a.code().equals("ability.fast-ship")).findFirst().orElseThrow();
            assertEquals(FastShipAbility.class, info.implementationClass());

            Ship ship = extensionContext.resolve(new Param("retail")).first(Ship.class);
            assertNotSame(FastShipAbility.class, ship.getClass(), "the bean is a Spring proxy");
            assertEquals("fast-ship", ship.ship());
        });
    }

    // ---- configuration

    @Test
    public void testStrictUnlessUnknownBusinessesAreAllowed() {
        runner.run(context -> {
            ResolutionException e = assertThrows(ResolutionException.class, () -> extensionContext(context).resolve(new Param("nobody")));
            assertEquals(ResolutionException.Reason.NO_BUSINESS_MATCHED, e.reason());
        });
        runner.withPropertyValues("easy-extension.allow-unknown-business=true").run(context ->
                assertEquals("default-pay", extensionContext(context).resolve(new Param("nobody")).first(Pay.class).pay()));
    }

    public static class SecondRetailPay implements Pay {
        @Override
        public String pay() {
            return "retail2-pay";
        }
    }

    @Configuration
    static class TwoRetailBusinesses {
        @Bean
        BusinessDefinition<Param> secondRetail() {
            return BusinessDefinition.<Param>of("biz.retail.2", param -> "retail".equals(param.tenant()), new SecondRetailPay());
        }
    }

    @Test
    public void testSeveralMatchingBusinessesIsAnErrorInEveryMode() {
        for (String allowUnknown : new String[]{"false", "true"}) {
            runner.withUserConfiguration(TwoRetailBusinesses.class)
                    .withPropertyValues("easy-extension.allow-unknown-business=" + allowUnknown)
                    .run(context -> {
                        ResolutionException e = assertThrows(ResolutionException.class, () -> extensionContext(context).resolve(new Param("retail")));
                        assertEquals(ResolutionException.Reason.MULTIPLE_BUSINESSES_MATCHED, e.reason(), "allow-unknown-business=" + allowUnknown);
                        assertEquals("multiple business found, matched business codes: [biz.retail, biz.retail.2]", e.getMessage(),
                                "allow-unknown-business=" + allowUnknown);
                    });
        }
    }

    @Test
    public void testALeftoverBusinessMatchOrderIsIgnored() {
        // removed in 5.0: an old configuration still starts, and does not settle several matching businesses any more
        runner.withUserConfiguration(TwoRetailBusinesses.class)
                .withPropertyValues("easy-extension.allow-unknown-business=true", "easy-extension.business-match-order=biz.retail.2,biz.retail")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    ResolutionException e = assertThrows(ResolutionException.class, () -> extensionContext(context).resolve(new Param("retail")));
                    assertEquals(ResolutionException.Reason.MULTIPLE_BUSINESSES_MATCHED, e.reason());
                    assertEquals("multiple business found, matched business codes: [biz.retail, biz.retail.2]", e.getMessage());
                });
    }

    @Test
    public void testMatcherParamTypeCanBeConfiguredAndMustFitTheMatchers() {
        runner.withPropertyValues("easy-extension.matcher-param-type=" + Param.class.getName()).run(context ->
                assertEquals(Param.class, extensionContext(context).catalog().matcherParamType()));
        runner.withPropertyValues("easy-extension.matcher-param-type=java.lang.String").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCause(context.getStartupFailure()).getMessage().contains("does not accept the configured matcher param type [java.lang.String]"),
                    rootCause(context.getStartupFailure()).getMessage());
        });
    }

    public static class ManualShip implements Ship {
        @Override
        public String ship() {
            return "manual-ship";
        }
    }

    @Configuration
    static class DefinitionsAsBeans {
        @Bean
        AbilityDefinition<Param> manualAbility() {
            return AbilityDefinition.of("ability.manual", param -> true, new ManualShip());
        }
    }

    @Test
    public void testProvidersCanBeDeclaredAsDefinitionBeansWithoutAnnotations() {
        runner.withUserConfiguration(DefinitionsAsBeans.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(extensionContext(context).catalog().abilities().stream().anyMatch(a -> a.code().equals("ability.manual")));
        });
    }

    @Configuration
    static class OwnContext {
        @Bean
        ExtensionContext<Param> ownContext() {
            return ExtensionContext.<Param>builder().extensionPoint(Pay.class).defaultImplementation(new DefaultPay())
                    .business(BusinessDefinition.<Param>of("biz.own", param -> true, new SecondRetailPay())).build();
        }
    }

    @Test
    public void testAnExtensionContextBeanOfYourOwnReplacesTheDeferredOne() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class))
                .withUserConfiguration(OwnContext.class).run(context -> {
                    assertNull(context.getStartupFailure());
                    assertFalse(context.getBean(ExtensionContext.class) instanceof DeferredExtensionContext);
                    try (Binding ignored = extensionContext(context).bind(new Param("any"))) {
                        assertEquals("retail2-pay", extensionContext(context).first(Pay.class).pay());
                    }
                });
    }
}
