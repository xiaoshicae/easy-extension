package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.wiring;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.BusinessMatchSelector;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.session.DefaultScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixtures for the container tests of how the starter wires the context: split default implementations, a mandatory
 * extension point, and the beans (interceptors, session manager, selector) it picks up.
 * Everything lives in this package because {@code @ExtensionScan} scans the package of the configuration class.
 */
public final class WiringFixtures {

    private WiringFixtures() {
    }

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @ExtensionPoint
    public interface Closer {
        String close();
    }

    /** Has no default: whoever serves the request has to provide it. */
    @ExtensionPoint(mandatory = true)
    public interface Auditor {
        String audit();
    }

    @MatcherParam
    public static class Param {
        public final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    /** The default of {@link Greeter}, and only of that: the defaults are split over two classes. */
    @ExtensionPointDefaultImplementation
    public static class GreeterDefault implements Greeter {
        @Override
        public String greet() {
            return "default greeting";
        }
    }

    /** The default of {@link Closer}. */
    @ExtensionPointDefaultImplementation
    public static class CloserDefault implements Closer {
        @Override
        public String close() {
            return "default closing";
        }
    }

    /** Matches "a" and "both". */
    @Business(code = "biz.a")
    public static class BizA implements Matcher<Param>, Greeter, Auditor {
        @Override
        public boolean match(Param param) {
            return param.name.equals("a") || param.name.equals("both");
        }

        @Override
        public String greet() {
            return "a greets";
        }

        @Override
        public String audit() {
            return "a audits";
        }
    }

    /** Matches "b" and "both". */
    @Business(code = "biz.b")
    public static class BizB implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return param.name.equals("b") || param.name.equals("both");
        }

        @Override
        public String greet() {
            return "b greets";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class BaseConfig {
    }

    public static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    /** Two interceptors, declared in the opposite order of their {@code @Order}. */
    @Configuration
    public static class InterceptorConfig {
        @Bean
        @Order(2)
        ExtensionInterceptor second() {
            return invocation -> {
                EVENTS.add("second before");
                try {
                    return invocation.proceed();
                } finally {
                    EVENTS.add("second after");
                }
            };
        }

        @Bean
        @Order(1)
        ExtensionInterceptor first() {
            return invocation -> {
                EVENTS.add("first before");
                try {
                    return invocation.proceed();
                } finally {
                    EVENTS.add("first after");
                }
            };
        }
    }

    public static final class CountingSessionManager extends DefaultScopedSessionManager {
        public static final AtomicInteger BINDS = new AtomicInteger();

        @Override
        public void bindScopedChain(String scope, ResolvedChain chain) throws io.github.xiaoshicae.extension.core.exception.SessionException {
            BINDS.incrementAndGet();
            super.bindScopedChain(scope, chain);
        }
    }

    @Configuration
    public static class SessionManagerConfig {
        @Bean
        IScopedSessionManager sessionManager() {
            return new CountingSessionManager();
        }
    }

    /** Picks the last of the matching businesses. */
    @Configuration
    public static class SelectorConfig {
        @Bean
        BusinessMatchSelector<Param> lastWins() {
            return (List<IBusiness<Param>> matched, Param param) -> matched.get(matched.size() - 1);
        }
    }

    /** A second selector: with {@link SelectorConfig} there are two, and neither is primary. */
    @Configuration
    public static class SecondSelectorConfig {
        @Bean
        BusinessMatchSelector<Param> firstWins() {
            return (List<IBusiness<Param>> matched, Param param) -> matched.get(0);
        }
    }

    /** A primary selector: with {@link SelectorConfig} there are two, and this one is the one to use. */
    @Configuration
    public static class PrimarySelectorConfig {
        @Bean
        @Primary
        BusinessMatchSelector<Param> primaryFirstWins() {
            return (List<IBusiness<Param>> matched, Param param) -> matched.get(0);
        }
    }

    /** A second session manager: with {@link SessionManagerConfig} there are two, and neither is primary. */
    @Configuration
    public static class SecondSessionManagerConfig {
        @Bean
        IScopedSessionManager anotherSessionManager() {
            return new DefaultScopedSessionManager();
        }
    }
}
