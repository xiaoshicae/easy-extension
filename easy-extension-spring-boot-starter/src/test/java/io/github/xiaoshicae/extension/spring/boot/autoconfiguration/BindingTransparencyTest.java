package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.EasyExtensionWebAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.ExtensionSessionInterceptor;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.MatcherParamResolver;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.DomainConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The binding of HTTP requests is not magic: what is registered, for which paths, where the identity may come from, and
 * what is logged about it. (Handing the binding to Spring task executors: {@link AsyncPropagationTest}.)
 */
public class BindingTransparencyTest {
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ch.qos.logback.classic.Logger[] watched = {
            logger(ExtensionSessionInterceptor.class), logger(EasyExtensionWebAutoConfiguration.class)};

    private static ch.qos.logback.classic.Logger logger(Class<?> type) {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(type);
    }

    @BeforeEach
    public void captureLogs() {
        logs.start();
        for (var logger : watched) {
            logger.setLevel(Level.DEBUG);
            logger.addAppender(logs);
        }
    }

    @AfterEach
    public void releaseLogs() {
        for (var logger : watched) {
            logger.detachAppender(logs);
            logger.setLevel(null);
        }
        logs.stop();
    }

    private List<String> messages() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    // ---- HTTP: what is bound, for which paths

    @RestController
    static class Probe {
        private final ExtensionContext<Param> context;

        Probe(ExtensionContext<Param> context) {
            this.context = context;
        }

        @GetMapping({"/api/probe", "/health/probe", "/other/probe"})
        String probe() {
            return context.isBound() ? String.valueOf(context.current().trace().matchedBusinessCode()) : "unbound";
        }
    }

    @Configuration
    @EnableWebMvc
    static class Mvc {
        @Bean
        Probe probe(ExtensionContext<Param> context) {
            return new Probe(context);
        }

        @Bean
        MatcherParamResolver<Param> resolver() {
            return request -> new Param(request.getHeader("X-Tenant"));
        }
    }

    private final WebApplicationContextRunner mvcRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
            .withUserConfiguration(DomainConfig.class, Mvc.class)
            .withPropertyValues("easy-extension.allow-unknown-business=true");

    private static MockMvc mvc(org.springframework.context.ApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
    }

    /** The body the probe answers with: the business bound to the request thread, or "unbound". */
    private static String probe(MockMvc mvc, String path, String tenant) throws Exception {
        return mvc.perform(get(path).header("X-Tenant", tenant)).andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testEveryRequestIsBoundByDefault() {
        mvcRunner.run(ctx -> {
            MockMvc mvc = mvc(ctx);
            assertEquals("biz.retail", probe(mvc, "/api/probe", "retail"));
            assertEquals("biz.retail", probe(mvc, "/other/probe", "retail"));
            // no business matches: still bound, to the defaults (null business), not "unbound"
            assertEquals("null", probe(mvc, "/api/probe", "nobody"));
        });
    }

    @Test
    public void testExcludedPathsAreNotBound() {
        mvcRunner.withPropertyValues("easy-extension.session-exclude-path-patterns=/health/**").run(ctx -> {
            MockMvc mvc = mvc(ctx);
            assertEquals("biz.retail", probe(mvc, "/api/probe", "retail"));
            assertEquals("unbound", probe(mvc, "/health/probe", "retail"));
        });
    }

    @Test
    public void testOnlyIncludedPathsAreBound() {
        mvcRunner.withPropertyValues("easy-extension.session-include-path-patterns=/api/**").run(ctx -> {
            MockMvc mvc = mvc(ctx);
            assertEquals("biz.retail", probe(mvc, "/api/probe", "retail"));
            assertEquals("unbound", probe(mvc, "/other/probe", "retail"));
        });
    }

    @Test
    public void testIncludeThenExclude() {
        mvcRunner.withPropertyValues("easy-extension.session-include-path-patterns=/api/**,/health/**",
                "easy-extension.session-exclude-path-patterns=/health/**").run(ctx -> {
            MockMvc mvc = mvc(ctx);
            assertEquals("biz.retail", probe(mvc, "/api/probe", "retail"));
            assertEquals("unbound", probe(mvc, "/health/probe", "retail"));
            assertEquals("unbound", probe(mvc, "/other/probe", "retail"));
        });
    }

    @Test
    public void testTheBindingEndsWithTheRequest() {
        mvcRunner.run(ctx -> {
            assertEquals("biz.retail", probe(mvc(ctx), "/api/probe", "retail"));

            assertFalse(ctx.getBean(ExtensionContext.class).isBound(), "nothing may stay bound on the test thread");
        });
    }

    @org.springframework.web.bind.annotation.RestControllerAdvice
    static class ExtensionErrors {
        @org.springframework.web.bind.annotation.ExceptionHandler(io.github.xiaoshicae.extension.core.exception.ResolutionException.class)
        org.springframework.http.ResponseEntity<String> handle(io.github.xiaoshicae.extension.core.exception.ResolutionException e) {
            return org.springframework.http.ResponseEntity.status(422).body(e.reason().name());
        }
    }

    @Test
    public void testAnUnmatchedBusinessCanBeAnsweredByAnExceptionHandler() {
        // strict mode (no allow-unknown-business): the interceptor fails the request, an advice decides the status
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class, Mvc.class, ExtensionErrors.class)
                .run(ctx -> {
                    MockMvc mvc = mvc(ctx);

                    var unmatched = mvc.perform(get("/api/probe").header("X-Tenant", "nobody")).andReturn().getResponse();
                    assertEquals(422, unmatched.getStatus());
                    assertEquals("NO_BUSINESS_MATCHED", unmatched.getContentAsString());
                    assertEquals("biz.retail", probe(mvc, "/api/probe", "retail"));
                });
    }

    // ---- endpoints that carry the business identity in different places

    @RestController
    static class Endpoints {
        private final ExtensionContext<Param> context;

        Endpoints(ExtensionContext<Param> context) {
            this.context = context;
        }

        private String business() {
            return context.isBound() ? String.valueOf(context.current().trace().matchedBusinessCode()) : "unbound";
        }

        @GetMapping("/orders/{tenant}/pay")
        String pay(@PathVariable("tenant") String tenant) {
            return business();
        }

        @GetMapping("/search")
        String search(@RequestParam("biz") String biz) {
            return business();
        }

        @GetMapping("/ship")
        String ship() {
            return business();
        }

        @GetMapping("/open")
        String open() {
            return business();
        }
    }

    /** One resolver for every endpoint: it reads what Spring MVC has worked out about the request before the interceptors run. */
    @Configuration
    @EnableWebMvc
    static class BranchingMvc {
        @Bean
        Endpoints endpoints(ExtensionContext<Param> context) {
            return new Endpoints(context);
        }

        @Bean
        @SuppressWarnings("unchecked")
        MatcherParamResolver<Param> resolver() {
            return request -> {
                String pattern = Objects.toString(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), "");
                Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                return switch (pattern) {
                    case "/orders/{tenant}/pay" -> new Param(pathVariables.get("tenant"));
                    case "/search" -> new Param(request.getParameter("biz"));
                    default -> new Param(request.getHeader("X-Tenant"));
                };
            };
        }
    }

    @Test
    public void testOneResolverCanReadTheIdentityWhereEachEndpointKeepsIt() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class, BranchingMvc.class)
                .withPropertyValues("easy-extension.allow-unknown-business=true")
                .run(ctx -> {
                    MockMvc mvc = mvc(ctx);

                    assertEquals("biz.retail", body(mvc, get("/orders/retail/pay")), "path variable");
                    assertEquals("biz.retail", body(mvc, get("/search").param("biz", "retail")), "query parameter");
                    assertEquals("biz.retail", body(mvc, get("/ship").header("X-Tenant", "retail")), "header");
                    assertEquals("null", body(mvc, get("/ship")), "no identity: bound, to the defaults");
                });
    }

    /** No resolver bean: each group of paths gets an interceptor of its own, all with the same param type. */
    @Configuration
    @EnableWebMvc
    static class PerGroupMvc implements WebMvcConfigurer {
        private final ExtensionContext<Param> context;

        PerGroupMvc(ExtensionContext<Param> context) {
            this.context = context;
        }

        @Bean
        Endpoints endpoints(ExtensionContext<Param> context) {
            return new Endpoints(context);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> new Param(request.getParameter("biz"))))
                    .addPathPatterns("/search");
            registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> new Param(request.getHeader("X-Tenant"))))
                    .addPathPatterns("/ship");
            registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> new Param(
                            ((Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).get("tenant"))))
                    .addPathPatterns("/orders/{tenant}/**");
        }
    }

    @Test
    public void testInterceptorsRegisteredByHandBindOnlyTheirOwnPaths() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class, PerGroupMvc.class)
                .withPropertyValues("easy-extension.allow-unknown-business=true")
                .run(ctx -> {
                    MockMvc mvc = mvc(ctx);

                    assertEquals("biz.retail", body(mvc, get("/orders/retail/pay")));
                    assertEquals("biz.retail", body(mvc, get("/search").param("biz", "retail")));
                    assertEquals("biz.retail", body(mvc, get("/ship").header("X-Tenant", "retail")));
                    assertEquals("unbound", body(mvc, get("/open").header("X-Tenant", "retail")), "covered by none of the interceptors");
                    assertFalse(ctx.getBean(ExtensionContext.class).isBound());
                });
    }

    @Configuration
    @EnableWebMvc
    static class TwoResolversMvc {
        @Bean
        MatcherParamResolver<Param> headerResolver() {
            return request -> new Param(request.getHeader("X-Tenant"));
        }

        @Bean
        MatcherParamResolver<Param> queryResolver() {
            return request -> new Param(request.getParameter("biz"));
        }
    }

    @Test
    public void testOnlyOneResolverBeanIsSupported() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class, TwoResolversMvc.class)
                .run(ctx -> {
                    Throwable failure = ctx.getStartupFailure();
                    assertNotNull(failure, "two resolvers are ambiguous: the application must not pick one silently");
                    while (failure.getCause() != null) {
                        failure = failure.getCause();
                    }
                    assertInstanceOf(NoUniqueBeanDefinitionException.class, failure);
                });
    }

    private static String body(MockMvc mvc, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getContentAsString();
    }

    // ---- what the logs say

    @Test
    public void testStartupSaysHowRequestsAreBound() {
        mvcRunner.withPropertyValues("easy-extension.session-exclude-path-patterns=/health/**").run(ctx -> {
            String message = messages().stream().filter(m -> m.contains("HTTP binding is on")).findFirst().orElseThrow();

            assertTrue(message.contains("[/**]"), message);
            assertTrue(message.contains("[/health/**]"), message);
            assertTrue(message.contains("MatcherParamResolver"), message);
        });
    }

    @Test
    public void testStartupSaysWhenNothingBindsRequests() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class)
                .run(ctx -> {
                    String message = messages().stream().filter(m -> m.contains("not bound to a business")).findFirst().orElseThrow();

                    assertTrue(message.contains("no MatcherParamResolver bean"), message);
                    assertTrue(message.contains("runWith"), message);
                    assertTrue(message.contains("ExtensionSessionInterceptor"), "hand-registered interceptors still bind: " + message);
                    assertTrue(messages().stream().noneMatch(m -> m.contains("HTTP binding is on")), messages().toString());
                });
    }

    @Test
    public void testEachRequestLogsWhatWasBound() {
        mvcRunner.run(ctx -> {
            assertEquals("biz.retail", probe(mvc(ctx), "/api/probe", "retail"));

            String message = messages().stream().filter(m -> m.contains("bound GET /api/probe")).findFirst().orElseThrow();
            assertTrue(message.contains("business [biz.retail]"), message);
        });
    }
}
