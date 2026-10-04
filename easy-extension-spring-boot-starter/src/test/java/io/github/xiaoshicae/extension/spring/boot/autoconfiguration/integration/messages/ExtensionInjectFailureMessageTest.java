package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.messages;

import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When the extension point is registered but something else goes wrong while the proxy for it is created, the error
 * must not tell the user that the extension point is not registered.
 */
public class ExtensionInjectFailureMessageTest {

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

    public static class Caller {
        @ExtensionInject
        Greeter greeter;
    }

    /** The application supplies its own extension context bean, and that one cannot be created. */
    @Configuration
    @ExtensionScan
    public static class Config {
        // declared first: it is created first, and needs the proxy of the extension point
        @Bean
        Caller caller() {
            return new Caller();
        }

        @Bean
        IExtensionContext<Param> extensionContext() {
            throw new IllegalStateException("the extension context cannot be created");
        }
    }

    @Test
    public void testFailureToCreateTheProxyIsNotReportedAsAMissingBean() {
        try (var ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(Config.class, EasyExtensionAutoConfiguration.class);

            BeanCreationException failure = assertThrows(BeanCreationException.class, ctx::refresh);

            boolean realCauseReported = false;
            for (Throwable t = failure; t != null; t = t.getCause()) {
                String message = String.valueOf(t.getMessage());
                assertFalse(message.contains("no bean ["), "the extension point is registered, the bean exists: " + message);
                realCauseReported |= message.contains("the extension context cannot be created");
            }
            assertTrue(realCauseReported, "the real cause is part of the exception chain");
        }
    }
}
