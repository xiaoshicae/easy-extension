package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.integration.web;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionScan;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.EasyExtensionWebAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.SessionCleanupFilter;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The session cleanup filter is there so that no request leaves a session behind on a container thread. The container
 * serves more than the first dispatch of a request on its threads (async dispatch, error dispatch), so the filter must
 * run for those too. Runs on an embedded Tomcat with a single worker thread, so every request is served by the same
 * thread and a session left behind by one is seen by the next.
 */
public class SessionCleanupFilterDispatchTest {

    @ExtensionPoint
    public interface Hello {
        String hello();
    }

    @MatcherParam
    public static class Who {
        public final String name;

        public Who(String name) {
            this.name = name;
        }
    }

    @ExtensionPointDefaultImplementation
    public static class HelloDefault implements Hello {
        @Override
        public String hello() {
            return "default hello";
        }
    }

    @Business(code = "biz.a")
    public static class BizA implements Matcher<Who>, Hello {
        @Override
        public boolean match(Who param) {
            return "a".equals(param.name);
        }

        @Override
        public String hello() {
            return "a says hello";
        }
    }

    /** Starts an async cycle and dispatches it to {@code /async-target}: what an async controller result amounts to. */
    private static final class AsyncStarter extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) {
            AsyncContext async = request.startAsync();
            async.dispatch("/async-target");
        }
    }

    /**
     * Code that runs on the async dispatch and starts the session of the request, the way a Spring MVC
     * {@code HandlerInterceptor.preHandle} does (the container calls it again on the async dispatch).
     */
    private static final class AsyncTarget extends HttpServlet {
        private final IExtensionContext<Who> context;

        private AsyncTarget(IExtensionContext<Who> context) {
            this.context = context;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            try {
                context.initSession(new Who("a"));
            } catch (SessionException e) {
                throw new IllegalStateException(e);
            }
            response.getWriter().write(context.invoke(Hello.class, Hello::hello));
        }
    }

    /** Starts the session of the request on the first dispatch, as a filter or an interceptor of the application does. */
    private static final class SyncTarget extends HttpServlet {
        private final IExtensionContext<Who> context;

        private SyncTarget(IExtensionContext<Who> context) {
            this.context = context;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            try {
                context.initSession(new Who("a"));
            } catch (SessionException e) {
                throw new IllegalStateException(e);
            }
            response.getWriter().write(context.invoke(Hello.class, Hello::hello));
        }
    }

    /** Fails: the container dispatches the request to the error page ({@code /error}) on one of its threads. */
    private static final class Boom extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
            throw new ServletException("boom");
        }
    }

    /** Says whether the thread that serves it still has a session. */
    private static final class Probe extends HttpServlet {
        private final IExtensionContext<Who> context;

        private Probe(IExtensionContext<Who> context) {
            this.context = context;
        }

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter().write(context.currentChain() == null ? "clean" : "a session of an earlier request");
        }
    }

    /** What an async controller method amounts to: the session is used on the request thread, the result is rendered later. */
    @RestController
    public static class AsyncController {
        private final IExtensionContext<Who> context;

        public AsyncController(IExtensionContext<Who> context) {
            this.context = context;
        }

        @GetMapping("/mvc-async")
        public CompletableFuture<String> hello() {
            return CompletableFuture.completedFuture(context.invoke(Hello.class, Hello::hello));
        }
    }

    /**
     * Starts the session of the request, as an application that resolves the business in an interceptor does. Spring MVC
     * calls {@code preHandle} again for the async dispatch of the request, on a container thread.
     */
    private static final class SessionStartingInterceptor implements HandlerInterceptor {
        private final IExtensionContext<Who> context;

        private SessionStartingInterceptor(IExtensionContext<Who> context) {
            this.context = context;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            context.initSession(new Who("a"));
            return true;
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @ExtensionScan
    public static class App {
        @Bean
        @SuppressWarnings("unchecked")
        AsyncController asyncController(IExtensionContext<?> context) {
            return new AsyncController((IExtensionContext<Who>) context);
        }

        @Bean
        @SuppressWarnings("unchecked")
        WebMvcConfigurer sessionStartingInterceptor(IExtensionContext<?> context) {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(new SessionStartingInterceptor((IExtensionContext<Who>) context)).addPathPatterns("/mvc-async", "/error");
                }
            };
        }

        @Bean
        ServletRegistrationBean<HttpServlet> asyncStarter() {
            return new ServletRegistrationBean<>(new AsyncStarter(), "/async");
        }

        @Bean
        @SuppressWarnings("unchecked")
        ServletRegistrationBean<HttpServlet> asyncTarget(IExtensionContext<?> context) {
            return new ServletRegistrationBean<>(new AsyncTarget((IExtensionContext<Who>) context), "/async-target");
        }

        @Bean
        @SuppressWarnings("unchecked")
        ServletRegistrationBean<HttpServlet> syncTarget(IExtensionContext<?> context) {
            return new ServletRegistrationBean<>(new SyncTarget((IExtensionContext<Who>) context), "/sync-target");
        }

        @Bean
        ServletRegistrationBean<HttpServlet> boom() {
            return new ServletRegistrationBean<>(new Boom(), "/boom");
        }

        @Bean
        @SuppressWarnings("unchecked")
        ServletRegistrationBean<HttpServlet> probe(IExtensionContext<?> context) {
            return new ServletRegistrationBean<>(new Probe((IExtensionContext<Who>) context), "/probe");
        }
    }

    private static String get(HttpClient client, int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(30)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    @Test
    public void testTheFilterIsMappedToTheDispatchesThatRunOnContainerThreads() {
        FilterRegistrationBean<SessionCleanupFilter> registration =
                new EasyExtensionWebAutoConfiguration().sessionCleanupFilterRegistration(new DefaultExtensionContext<>());

        // FORWARD and INCLUDE run inside a dispatch that is covered already: cleaning up after them would end the
        // session of the code that forwards
        assertEquals(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR), registration.determineDispatcherTypes());
    }

    private static ConfigurableApplicationContext startServer() {
        return new SpringApplicationBuilder(App.class)
                .web(WebApplicationType.SERVLET)
                .properties("server.port=0", "server.tomcat.threads.max=1", "server.tomcat.threads.min-spare=1",
                        "spring.main.banner-mode=off")
                .run();
    }

    @Test
    public void testASessionStartedOnTheFirstDispatchDoesNotOutliveTheRequest() throws Exception {
        try (ConfigurableApplicationContext app = startServer()) {
            int port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
            HttpClient client = HttpClient.newHttpClient();

            assertEquals("a says hello", get(client, port, "/sync-target"));

            assertEquals("clean", get(client, port, "/probe"), "the worker thread was cleaned after the request that used it");
        }
    }

    @Test
    public void testASessionStartedOnAnAsyncDispatchDoesNotOutliveTheRequest() throws Exception {
        try (ConfigurableApplicationContext app = startServer()) {
            int port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
            HttpClient client = HttpClient.newHttpClient();

            assertEquals("a says hello", get(client, port, "/async"));

            assertEquals("clean", get(client, port, "/probe"), "the worker thread was cleaned after the request that used it");
        }
    }

    @Test
    public void testASessionStartedByAnInterceptorOfAnAsyncControllerMethodDoesNotOutliveTheRequest() throws Exception {
        try (ConfigurableApplicationContext app = startServer()) {
            int port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
            HttpClient client = HttpClient.newHttpClient();

            assertEquals("a says hello", get(client, port, "/mvc-async"));

            assertEquals("clean", get(client, port, "/probe"), "the worker thread was cleaned after the request that used it");
        }
    }

    @Test
    public void testASessionStartedByAnInterceptorOnTheErrorDispatchDoesNotOutliveTheRequest() throws Exception {
        try (ConfigurableApplicationContext app = startServer()) {
            int port = Integer.parseInt(app.getEnvironment().getProperty("local.server.port"));
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/boom")).timeout(Duration.ofSeconds(30)).GET().build();
            assertEquals(500, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());

            assertEquals("clean", get(client, port, "/probe"), "the worker thread was cleaned after the request that used it");
        }
    }
}
