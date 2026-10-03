package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.cache;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixtures for container-level tests. Everything lives in this package because
 * {@code @ExtensionScan} scans the package of the configuration class that carries it.
 *
 * <p>The only thing that differs between the three configurations is whether Spring wraps the business and the
 * ability in an AOP proxy: {@code @Cacheable} is inert in {@link PlainConfig}, produces a JDK interface proxy in
 * {@link JdkProxyConfig} and a CGLIB subclass proxy in {@link CglibProxyConfig} (the Spring Boot default).</p>
 */
public final class CacheFixtures {

    private CacheFixtures() {
    }

    @ExtensionPoint
    public interface RateExt {
        String rate();
    }

    @ExtensionPoint
    public interface PromoExt {
        String promo();
    }

    @MatcherParam
    public static class Param {
    }

    @ExtensionPointDefaultImplementation
    public static class DefaultImpl implements RateExt, PromoExt {
        @Override
        public String rate() {
            return "default-rate";
        }

        @Override
        public String promo() {
            return "default-promo";
        }
    }

    @Ability(code = "ability.promo")
    public static class PromoAbility implements Matcher<Param>, PromoExt {
        public static final AtomicInteger CREATED = new AtomicInteger();
        public static final AtomicInteger PROMO_CALLS = new AtomicInteger();

        public PromoAbility() {
            CREATED.incrementAndGet();
        }

        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        @Cacheable("promos")
        public String promo() {
            PROMO_CALLS.incrementAndGet();
            return "promo-ability";
        }
    }

    // business priority defaults to 0; the mounted ability is auto-assigned priority 1 (declaration order)
    @Business(code = "biz.retail", abilities = {"ability.promo"})
    public static class RetailBusiness implements Matcher<Param>, RateExt {
        public static final AtomicInteger CREATED = new AtomicInteger();
        public static final AtomicInteger RATE_CALLS = new AtomicInteger();

        public RetailBusiness() {
            CREATED.incrementAndGet();
        }

        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        @Cacheable("rates")
        public String rate() {
            RATE_CALLS.incrementAndGet();
            return "retail-rate";
        }
    }

    public static void resetCounters() {
        PromoAbility.CREATED.set(0);
        PromoAbility.PROMO_CALLS.set(0);
        RetailBusiness.CREATED.set(0);
        RetailBusiness.RATE_CALLS.set(0);
    }

    /** No caching: nothing is proxied. */
    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class PlainConfig {
    }

    /** {@code @EnableCaching} with default settings: classes that implement interfaces get a JDK proxy. */
    @Configuration
    @EnableCaching
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class JdkProxyConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    /** {@code proxyTargetClass = true}: what Spring Boot applies by default, i.e. a CGLIB subclass proxy. */
    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class CglibProxyConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
