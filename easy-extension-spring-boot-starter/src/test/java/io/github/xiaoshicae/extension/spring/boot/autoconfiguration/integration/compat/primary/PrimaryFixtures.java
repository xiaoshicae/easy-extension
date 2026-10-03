package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.primary;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Default implementations that are beans of their own. With 3.3 only one could be injected, so {@code @Primary} was
 * the way to choose between several, a decorator of the real default, for instance.
 */
public final class PrimaryFixtures {

    private PrimaryFixtures() {
    }

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @ExtensionPoint
    public interface Closer {
        String close();
    }

    @MatcherParam
    public static class Param {
        public final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    @Business(code = "biz.a")
    public static class BizA implements Matcher<Param>, Greeter, Closer {
        @Override
        public boolean match(Param param) {
            return "a".equals(param.name);
        }

        @Override
        public String greet() {
            return "a greets";
        }

        @Override
        public String close() {
            return "a closes";
        }
    }

    /** Both extension points. */
    public static class RealDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Greeter, Closer {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Greeter.class, Closer.class);
        }

        @Override
        public String greet() {
            return "real default greeting";
        }

        @Override
        public String close() {
            return "real default closing";
        }
    }

    /** Decorates the real default: implements the same extension points. */
    public static class CachingDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Greeter, Closer {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Greeter.class, Closer.class);
        }

        @Override
        public String greet() {
            return "caching default greeting";
        }

        @Override
        public String close() {
            return "caching default closing";
        }
    }

    /** One default per extension point. */
    public static class GreeterOnlyDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Greeter {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Greeter.class);
        }

        @Override
        public String greet() {
            return "greeter default";
        }
    }

    public static class CloserOnlyDefault extends AbstractExtensionPointDefaultImplementation<Param> implements Closer {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Closer.class);
        }

        @Override
        public String close() {
            return "closer default";
        }
    }

    /** The real default and its {@code @Primary} decorator. */
    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class PrimaryConfig {
        @Bean
        RealDefault realDefault() {
            return new RealDefault();
        }

        @Bean
        @Primary
        CachingDefault cachingDefault() {
            return new CachingDefault();
        }
    }

    /** Two defaults for different extension points, none primary: both are used. */
    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class SplitConfig {
        @Bean
        GreeterOnlyDefault greeterDefault() {
            return new GreeterOnlyDefault();
        }

        @Bean
        CloserOnlyDefault closerDefault() {
            return new CloserOnlyDefault();
        }
    }
}
