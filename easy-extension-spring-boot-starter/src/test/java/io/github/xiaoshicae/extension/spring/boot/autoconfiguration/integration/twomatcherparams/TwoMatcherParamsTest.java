package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.twomatcherparams;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An extension context has one matcher parameter type. Two classes annotated with {@code @MatcherParam}, even of the
 * same simple name in different packages, are an error the application is told about, naming both.
 */
public class TwoMatcherParamsTest {

    @ExtensionPoint
    public interface Greeter {
        String greet();
    }

    @ExtensionPointDefaultImplementation
    public static class GreeterDefault implements Greeter {
        @Override
        public String greet() {
            return "default";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @Test
    public void testTwoMatcherParamClassesOfTheSameSimpleNameAreReportedByName() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.setAllowBeanDefinitionOverriding(false); // the Spring Boot default
            ctx.register(Config.class);

            BeanCreationException failure = assertThrows(BeanCreationException.class, ctx::refresh);

            Throwable root = failure;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            RegisterParamException e = assertInstanceOf(RegisterParamException.class, root);
            assertTrue(e.getMessage().startsWith("More than one instance annotated with @MatcherParam found"), e.getMessage());
            assertTrue(e.getMessage().contains(".twomatcherparams.a.Param") && e.getMessage().contains(".twomatcherparams.b.Param"), e.getMessage());
        }
    }
}
