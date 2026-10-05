package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.EasyExtensionWebAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.ExtensionSessionInterceptor;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.MatcherParamResolver;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.web.SessionCleanupFilter;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.*;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.DomainConfig;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class WebTest {

    // ---- the interceptor

    private final ExtensionContext<Param> context = ExtensionContext.<Param>builder()
            .extensionPoint(Pay.class, Ship.class)
            .defaultImplementation(new DefaultPay()).defaultImplementation(new DefaultShip())
            .ability(new FastShipAbility()).business(new RetailBusiness()).strict(false).build();
    private final MatcherParamResolver<Param> resolver = request -> new Param(request.getHeader("X-Tenant"));
    private final ExtensionSessionInterceptor<Param> interceptor = new ExtensionSessionInterceptor<>(context, resolver);
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    private static MockHttpServletRequest request(String tenant) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", tenant);
        return request;
    }

    private void assertNothingBound() {
        assertEquals(ResolutionException.Reason.NO_BINDING, assertThrows(ResolutionException.class, context::current).reason());
    }

    @Test
    public void testTheRequestIsBoundWhileItIsHandled() throws Exception {
        MockHttpServletRequest request = request("retail");

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals("retail-pay", context.first(Pay.class).pay());
        assertEquals("fast-ship", context.first(Ship.class).ship());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNothingBound();
    }

    @Test
    public void testAsyncHandlingReleasesTheThreadAndEveryDispatchBindsAgain() throws Exception {
        MockHttpServletRequest request = request("retail");

        interceptor.preHandle(request, response, new Object());
        // the handler returned a DeferredResult: the work continues on another thread, this one must be clean
        interceptor.afterConcurrentHandlingStarted(request, response, new Object());
        assertNothingBound();

        // the async dispatch comes back through the interceptor
        interceptor.preHandle(request, response, new Object());
        assertEquals("retail-pay", context.first(Pay.class).pay());
        interceptor.afterCompletion(request, response, new Object(), null);
        assertNothingBound();
    }

    @Test
    public void testNestedDispatchesOfOneRequestUnwindInOrder() throws Exception {
        MockHttpServletRequest request = request("retail");

        interceptor.preHandle(request, response, new Object());
        request.removeHeader("X-Tenant");
        request.addHeader("X-Tenant", "nobody");
        interceptor.preHandle(request, response, new Object());                  // e.g. a forward
        assertEquals("default-pay", context.first(Pay.class).pay());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertEquals("retail-pay", context.first(Pay.class).pay());
        interceptor.afterCompletion(request, response, new Object(), null);
        assertNothingBound();
    }

    @Test
    public void testAResolverFailureBindsNothing() {
        ExtensionSessionInterceptor<Param> failing = new ExtensionSessionInterceptor<>(context, request -> {
            throw new IllegalArgumentException("no tenant header");
        });
        assertThrows(IllegalArgumentException.class, () -> failing.preHandle(request("x"), response, new Object()));
        assertNothingBound();
    }

    @Test
    public void testTheErrorDispatchIsNeverBound() {
        ExtensionContext<Param> strict = ExtensionContext.<Param>builder()
                .extensionPoint(Pay.class, Ship.class)
                .defaultImplementation(new DefaultPay()).defaultImplementation(new DefaultShip())
                .ability(new FastShipAbility()).business(new RetailBusiness()).build();
        ExtensionSessionInterceptor<Param> interceptor = new ExtensionSessionInterceptor<>(strict, resolver);
        MockHttpServletRequest error = request("nobody");
        error.setDispatcherType(DispatcherType.ERROR);

        // no business matches "nobody": the request itself fails, but rendering /error must not fail again
        assertTrue(interceptor.preHandle(error, response, new Object()));
        interceptor.afterCompletion(error, response, new Object(), null);
        assertEquals(ResolutionException.Reason.NO_BINDING, assertThrows(ResolutionException.class, strict::current).reason());
    }

    @Test
    public void testCompletionWithoutAnyBindingIsHarmless() {
        interceptor.afterCompletion(request("retail"), response, new Object(), null);
        assertNothingBound();
    }

    // ---- the safety-net filter

    @Test
    public void testTheFilterDropsBindingsLeftOnTheThread() {
        SessionCleanupFilter filter = new SessionCleanupFilter(context);

        assertThrows(ServletException.class, () -> filter.doFilter(request("retail"), response, (req, res) -> {
            context.bind(new Param("retail"));        // never closed
            throw new ServletException("handler failed");
        }));
        assertNothingBound();
    }

    // ---- auto-configuration

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
            .withUserConfiguration(DomainConfig.class)
            .withPropertyValues("easy-extension.allow-unknown-business=true");

    @Configuration
    static class WithResolver {
        @Bean
        MatcherParamResolver<Param> matcherParamResolver() {
            return request -> new Param(request.getHeader("X-Tenant"));
        }
    }

    @Test
    public void testASafetyNetFilterIsRegisteredByDefault() {
        webRunner.run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(FilterRegistrationBean.class).size());
            assertTrue(ctx.getBeansOfType(WebMvcConfigurer.class).isEmpty(), "no interceptor without a MatcherParamResolver");
        });
        webRunner.withPropertyValues("easy-extension.enable-session-auto-cleanup=false")
                .run(ctx -> assertTrue(ctx.getBeansOfType(FilterRegistrationBean.class).isEmpty()));
    }

    @Test
    public void testAMatcherParamResolverBeanTurnsOnAutomaticBinding() {
        webRunner.withUserConfiguration(WithResolver.class).run(ctx -> {
            assertNull(ctx.getStartupFailure());
            assertEquals(1, ctx.getBeansOfType(WebMvcConfigurer.class).size());
        });
    }

    @Test
    public void testNothingWebIsRegisteredOutsideAServletApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, EasyExtensionWebAutoConfiguration.class))
                .withUserConfiguration(DomainConfig.class)
                .run(ctx -> {
                    assertNull(ctx.getStartupFailure());
                    assertTrue(ctx.getBeansOfType(FilterRegistrationBean.class).isEmpty());
                });
    }
}
