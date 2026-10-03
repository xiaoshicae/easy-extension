package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.CglibProxyConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.JdkProxyConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.PlainConfig;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.PromoAbility;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.PromoExt;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.RateExt;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache.CacheFixtures.RetailBusiness;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ClassHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.InstanceHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Container-level tests: a real {@link AnnotationConfigApplicationContext} with component scanning, as opposed to
 * calling the auto-configuration methods by hand. These are the scenarios that went unnoticed before:
 * extension implementations that Spring wraps in an AOP proxy, and implementations created twice.
 */
public class SpringIntegrationTest {

    @BeforeEach
    public void resetCounters() {
        CacheFixtures.resetCounters();
    }

    @SuppressWarnings("unchecked")
    private static IExtensionContext<Param> extensionContext(AnnotationConfigApplicationContext ctx) {
        return ctx.getBean(IExtensionContext.class);
    }

    /**
     * The bean whose target class is {@code type}. Looked up through {@link Matcher}, which both fixtures implement,
     * because behind a JDK proxy the bean is no longer an instance of the fixture class itself.
     */
    private static Object beanOf(AnnotationConfigApplicationContext ctx, Class<?> type) {
        assertEquals(2, ctx.getBeanNamesForType(Matcher.class).length, "business and ability implement Matcher");
        for (String name : ctx.getBeanNamesForType(Matcher.class)) {
            Object bean = ctx.getBean(name);
            if (AopUtils.getTargetClass(bean) == type) {
                return bean;
            }
        }
        throw new AssertionError("no bean with target class " + type.getName());
    }

    private static Object businessBean(AnnotationConfigApplicationContext ctx) {
        return beanOf(ctx, RetailBusiness.class);
    }

    @Test
    public void testPlainBeansAreInstantiatedOnceAndRegisteredAsTheSpringBean() {
        try (var ctx = new AnnotationConfigApplicationContext(PlainConfig.class)) {
            assertEquals(1, RetailBusiness.CREATED.get(), "business must be created once, not once as a bean and once more by the framework");
            assertEquals(1, PromoAbility.CREATED.get(), "ability must be created once");

            IExtensionContext<Param> ec = extensionContext(ctx);
            assertEquals(1, ec.listAllBusiness().size());
            assertEquals(1, ec.listAllAbility().size());

            IBusiness<Param> registered = ec.listAllBusiness().get(0);
            Object registeredInstance = ((IProxy<?>) registered).getInstance();
            assertSame(businessBean(ctx), registeredInstance,
                    "the instance used by the framework must be the very bean the application context manages");
            Object registeredAbility = ((IProxy<?>) ec.listAllAbility().get(0)).getInstance();
            assertSame(beanOf(ctx, PromoAbility.class), registeredAbility,
                    "the instance used by the framework must be the very bean the application context manages");
        }
    }

    @Test
    public void testBusinessAndAbilityBehindJdkProxyAreRegistered() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext(JdkProxyConfig.class)) {
            assertTrue(AopUtils.isJdkDynamicProxy(businessBean(ctx)), "precondition: Spring wrapped the business in a JDK proxy");
            assertTrue(AopUtils.isJdkDynamicProxy(beanOf(ctx, PromoAbility.class)), "precondition: Spring wrapped the ability in a JDK proxy");

            IExtensionContext<Param> ec = extensionContext(ctx);
            assertEquals(1, ec.listAllBusiness().size(), "business behind a proxy must still be registered");
            assertEquals(1, ec.listAllAbility().size(), "ability behind a proxy must still be registered");
            assertEquals(RetailBusiness.class, ((IProxy<?>) ec.listAllBusiness().get(0)).getTargetClass());
            assertEquals(PromoAbility.class, ((IProxy<?>) ec.listAllAbility().get(0)).getTargetClass());

            ec.initSession(new Param());
            try {
                assertEquals("retail-rate", ec.invoke(RateExt.class, RateExt::rate));
                assertEquals("promo-ability", ec.invoke(PromoExt.class, PromoExt::promo));
            } finally {
                ec.removeSession();
            }
        }
    }

    @Test
    public void testBusinessAndAbilityBehindCglibProxyAreRegistered() throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext(CglibProxyConfig.class)) {
            assertTrue(AopUtils.isCglibProxy(businessBean(ctx)), "precondition: Spring wrapped the business in a CGLIB proxy");
            assertTrue(AopUtils.isCglibProxy(beanOf(ctx, PromoAbility.class)), "precondition: Spring wrapped the ability in a CGLIB proxy");

            IExtensionContext<Param> ec = extensionContext(ctx);
            assertEquals(1, ec.listAllBusiness().size(), "business behind a proxy must still be registered");
            assertEquals(1, ec.listAllAbility().size(), "ability behind a proxy must still be registered");
            assertEquals(RetailBusiness.class, ((IProxy<?>) ec.listAllBusiness().get(0)).getTargetClass());

            ec.initSession(new Param());
            try {
                assertEquals("retail-rate", ec.invoke(RateExt.class, RateExt::rate));
                assertEquals("promo-ability", ec.invoke(PromoExt.class, PromoExt::promo));
            } finally {
                ec.removeSession();
            }
        }
    }

    @Test
    public void testSpringAdviceStillAppliesWhenTheFrameworkInvokesTheExtension() throws Exception {
        // The framework has to register the proxy, not the unwrapped target; otherwise @Cacheable (and likewise
        // @Transactional, @Async, aspects ...) would silently stop working for extension implementations.
        try (var ctx = new AnnotationConfigApplicationContext(CglibProxyConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);
            ec.initSession(new Param());
            try {
                assertEquals("retail-rate", ec.invoke(RateExt.class, RateExt::rate));
                assertEquals("retail-rate", ec.invoke(RateExt.class, RateExt::rate));
                assertEquals("promo-ability", ec.invoke(PromoExt.class, PromoExt::promo));
                assertEquals("promo-ability", ec.invoke(PromoExt.class, PromoExt::promo));
            } finally {
                ec.removeSession();
            }
            assertEquals(1, RetailBusiness.RATE_CALLS.get(), "second call must be served from the cache");
            assertEquals(1, PromoAbility.PROMO_CALLS.get(), "second call must be served from the cache");
        }
    }

    @Test
    public void testWithoutCachingTheExtensionIsCalledEveryTime() throws Exception {
        // Control for the previous test: proves the counter really measures "the implementation was invoked".
        try (var ctx = new AnnotationConfigApplicationContext(PlainConfig.class)) {
            IExtensionContext<Param> ec = extensionContext(ctx);
            ec.initSession(new Param());
            try {
                ec.invoke(RateExt.class, RateExt::rate);
                ec.invoke(RateExt.class, RateExt::rate);
            } finally {
                ec.removeSession();
            }
            assertEquals(2, RetailBusiness.RATE_CALLS.get());
            assertFalse(AopUtils.isAopProxy(businessBean(ctx)));
        }
    }

    @Test
    public void testScannedBeanThatCannotBeRegisteredFailsFast() {
        EasyExtensionAutoConfiguration<Object> configuration = new EasyExtensionAutoConfiguration<>();
        configuration.setExtensionPointHolders(List.of(new ExtensionPointHolder(SomeExt.class)));
        configuration.setClassHolders(List.of(new ClassHolder(SomeParam.class)));
        // A bean that was scanned as an extension component but carries none of the registration annotations
        // (for example because only an opaque proxy is visible): used to be skipped silently.
        configuration.setInstanceHolders(List.of(new InstanceHolder(new Object())));

        RegisterParamException e = assertThrows(RegisterParamException.class,
                () -> configuration.registerExtensionContext(new EasyExtensionConfigurationProperties()));
        assertTrue(e.getMessage().contains("carries none of @Ability, @Business or @ExtensionPointDefaultImplementation"), e.getMessage());
        assertTrue(e.getMessage().contains(Object.class.getName()), e.getMessage());
    }

    @ExtensionPoint
    public interface SomeExt {
    }

    @MatcherParam
    public static class SomeParam {
    }
}
