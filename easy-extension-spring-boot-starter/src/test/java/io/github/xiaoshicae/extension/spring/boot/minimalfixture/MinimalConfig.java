package io.github.xiaoshicae.extension.spring.boot.minimalfixture;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/**
 * The domain of the smallest setup: businesses are identified by a code and have no {@code Matcher}, there are no
 * abilities. Kept in a package of its own so that no other test scans it.
 */
@Configuration
@AutoConfigurationPackage
public class MinimalConfig {

    @ExtensionPoint
    public interface Greeting {
        String hello();
    }

    @DefaultImplementation
    public static class DefaultGreeting implements Greeting {
        @Override
        public String hello() {
            return "hello from default";
        }
    }

    @Business(code = "biz.retail")
    public static class Retail implements Greeting {
        @Override
        public String hello() {
            return "hello from retail";
        }
    }

    @Business(code = "biz.fresh")
    public static class Fresh implements Greeting {
        @Override
        public String hello() {
            return "hello from fresh";
        }
    }
}
