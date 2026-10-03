package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.scanned;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.compat.shared.Outside;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * An application that implements an extension point of a module it does not scan ({@link Outside}), directly in a
 * default implementation and by inheritance in a business. It started with 3.3, and must keep starting.
 */
public final class ScannedFixtures {

    private ScannedFixtures() {
    }

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @MatcherParam
    public static class Param {
        public final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    /** Implements an extension point nobody registers, next to the one that is. */
    @ExtensionPointDefaultImplementation
    public static class GreeterDefault implements Greeter, Outside {
        @Override
        public String greet() {
            return "default greeting";
        }

        @Override
        public String outside() {
            return "default outside";
        }
    }

    /** A shared base class that implements the extension point of the shared module. */
    public abstract static class OutsideBase implements Matcher<Param>, Outside {
        @Override
        public String outside() {
            return "base outside";
        }
    }

    @Business(code = "biz.inheriting")
    public static class Inheriting extends OutsideBase implements Greeter {
        @Override
        public boolean match(Param param) {
            return "inheriting".equals(param.name);
        }

        @Override
        public String greet() {
            return "inheriting greets";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class ScannedConfig {
    }
}
