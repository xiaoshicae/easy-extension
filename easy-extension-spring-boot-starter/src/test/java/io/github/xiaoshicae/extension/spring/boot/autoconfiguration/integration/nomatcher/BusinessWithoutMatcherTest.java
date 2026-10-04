package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.nomatcher;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
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
 * The start fails when a business forgets to implement {@code Matcher}; with dozens of businesses the message has to
 * say which one.
 */
public class BusinessWithoutMatcherTest {

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

    @Business(code = "biz.good")
    public static class Good implements Matcher<Param>, Greeter {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "good";
        }
    }

    /** Forgot to implement Matcher. */
    @Business(code = "biz.forgetful")
    public static class Forgetful implements Greeter {
        @Override
        public String greet() {
            return "forgetful";
        }
    }

    @Configuration
    @ExtensionScan
    @Import(EasyExtensionAutoConfiguration.class)
    public static class Config {
    }

    @Test
    public void testMessageNamesTheBusinessThatDoesNotImplementMatcher() {
        BeanCreationException failure = assertThrows(BeanCreationException.class, () -> new AnnotationConfigApplicationContext(Config.class));

        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        RegisterParamException e = assertInstanceOf(RegisterParamException.class, root);
        assertTrue(e.getMessage().startsWith("instance annotated with @Business should implement Matcher interface"), e.getMessage());
        assertTrue(e.getMessage().contains(Forgetful.class.getName()), e.getMessage());
    }
}
