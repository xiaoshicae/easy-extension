package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.EasyExtensionWebAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.MatcherParamResolver;
import io.github.xiaoshicae.extension.spring.boot.minimalfixture.MinimalConfig;
import io.github.xiaoshicae.extension.spring.boot.minimalfixture.MinimalConfig.Greeting;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The smallest setup the README shows: the business is a code in a request header ({@code T = String}), every
 * business matches its own code, and endpoints that do not use extension points stay out of the framework.
 */
public class MinimalSetupTest {

    @RestController
    static class Endpoints {
        @ExtensionInject
        private Greeting greeting;

        @GetMapping("/hello")
        String hello() {
            return greeting.hello();
        }

        /** A plain HTTP endpoint: it never touches an extension point. */
        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }

    @RestControllerAdvice
    static class Errors {
        @ExceptionHandler(ResolutionException.class)
        ResponseEntity<String> handle(ResolutionException e) {
            return ResponseEntity.status(422).body(e.reason().name());
        }
    }

    /** What the user writes: the resolver bean is all it takes (plus a match per business, see MinimalConfig). */
    @Configuration
    @EnableWebMvc
    static class Minimal {
        @Bean
        MatcherParamResolver<String> resolver() {
            return request -> request.getHeader("X-Biz-Code");
        }

        @Bean
        Endpoints endpoints() {
            return new Endpoints();
        }

        @Bean
        Errors errors() {
            return new Errors();
        }
    }

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
            .withUserConfiguration(MinimalConfig.class, Minimal.class);

    private static MockMvc mvc(org.springframework.context.ApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
    }

    private static String answer(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        return response.getStatus() + " " + response.getContentAsString();
    }

    @Test
    public void testTheBusinessCodeInTheHeaderPicksTheImplementation() {
        runner.run(ctx -> {
            MockMvc mvc = mvc(ctx);

            assertEquals("200 hello from retail", answer(mvc, get("/hello").header("X-Biz-Code", "biz.retail")));
            assertEquals("200 hello from fresh", answer(mvc, get("/hello").header("X-Biz-Code", "biz.fresh")));
        });
    }

    @Test
    public void testNoCodeOrAnUnknownCodeIsAnErrorInStrictMode() {
        runner.run(ctx -> {
            MockMvc mvc = mvc(ctx);

            assertEquals("422 NO_BUSINESS_MATCHED", answer(mvc, get("/hello")));
            assertEquals("422 NO_BUSINESS_MATCHED", answer(mvc, get("/hello").header("X-Biz-Code", "biz.other")));
        });
    }

    @Test
    public void testUnknownBusinessesFallBackToTheDefaultsWhenAllowed() {
        runner.withPropertyValues("easy-extension.allow-unknown-business=true").run(ctx -> {
            MockMvc mvc = mvc(ctx);

            assertEquals("200 hello from retail", answer(mvc, get("/hello").header("X-Biz-Code", "biz.retail")));
            assertEquals("200 hello from default", answer(mvc, get("/hello")));
            assertEquals("200 hello from default", answer(mvc, get("/hello").header("X-Biz-Code", "biz.other")));
        });
    }

    @Test
    public void testAnEndpointInsideTheBoundPathsNeedsAnIdentityEvenIfItIgnoresExtensionPoints() {
        runner.run(ctx -> assertEquals("422 NO_BUSINESS_MATCHED", answer(mvc(ctx), get("/ping"))));
    }

    @Test
    public void testPlainEndpointsStayOutOfTheFrameworkWhenExcluded() {
        runner.withPropertyValues("easy-extension.session-exclude-path-patterns=/ping").run(ctx -> {
            MockMvc mvc = mvc(ctx);

            assertEquals("200 pong", answer(mvc, get("/ping")));
            assertEquals("422 NO_BUSINESS_MATCHED", answer(mvc, get("/hello")));
            assertEquals("200 hello from retail", answer(mvc, get("/hello").header("X-Biz-Code", "biz.retail")));
        });
    }

    @Test
    public void testOnlyTheIncludedPathsAreBound() {
        runner.withPropertyValues("easy-extension.session-include-path-patterns=/hello").run(ctx -> {
            MockMvc mvc = mvc(ctx);

            assertEquals("200 pong", answer(mvc, get("/ping")));
            assertEquals("200 hello from fresh", answer(mvc, get("/hello").header("X-Biz-Code", "biz.fresh")));
        });
    }
}
