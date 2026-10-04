package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlaptwob;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.scanoverlaptwoa.TwoExtensionScansTest.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.springframework.context.annotation.Configuration;

/** The second module: its own extension point and business, its own scan configuration. */
public final class ModuleB {
    private ModuleB() {
    }

    @ExtensionPoint(mandatory = true)
    public interface Farewell {
        String bye();
    }

    @Business(code = "biz.b")
    public static class BizB implements Matcher<Param>, Farewell {
        @Override
        public boolean match(Param param) {
            return false;
        }

        @Override
        public String bye() {
            return "b";
        }
    }

    @Configuration
    @ExtensionScan
    public static class ModuleBConfig {
    }
}
