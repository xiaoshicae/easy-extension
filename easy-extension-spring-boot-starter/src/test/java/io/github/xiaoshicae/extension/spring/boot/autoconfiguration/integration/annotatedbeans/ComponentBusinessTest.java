package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.annotatedbeans;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A business that is also a {@code @Component} (so the application's own {@code @ComponentScan} registers it) must
 * still take part in matching.
 */
public class ComponentBusinessTest {

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @MatcherParam
    public static class Param {
    }

    @ExtensionPointDefaultImplementation
    public static class GreeterDefault implements Greeter {
        @Override
        public String greet() {
            return "default";
        }
    }

    @Component
    @Business(code = "biz.component")
    public static class ComponentBusiness implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "component";
        }
    }

    @Configuration
    @ComponentScan
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBusinessThatIsAlsoAComponentIsRegistered() {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            assertEquals(1, ctx.getBeansOfType(ComponentBusiness.class).size(), "precondition: it is a bean, exactly once");

            List<String> codes = ((IExtensionContext<Param>) ctx.getBean(IExtensionContext.class)).listAllBusiness().stream().map(IBusiness::code).toList();
            assertEquals(List.of("biz.component"), codes);
        }
    }
}
