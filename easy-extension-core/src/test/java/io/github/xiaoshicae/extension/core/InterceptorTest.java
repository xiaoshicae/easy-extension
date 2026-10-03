package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.Shipping;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interceptors wrap every call to an extension implementation that goes through the context: first-matched,
 * all-matched, the invoke family, and the proxies behind {@code @ExtensionInject}.
 */
public class InterceptorTest {

    private final List<String> events = new ArrayList<>();
    private DefaultExtensionContext<Req> context;
    private ShopBusiness retail;

    @BeforeEach
    public void setUp() throws Exception {
        context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        retail = ShopBusiness.named("retail", Pricing.class, Tax.class);
        context.registerBusiness(retail);
        context.initSession(new Req("retail"));
    }

    private void tearDown() {
        context.removeSession();
    }

    @Test
    public void testWithoutInterceptorsTheRegisteredImplementationIsHandedOutAsItIs() throws Exception {
        try {
            assertSame(retail, context.getFirstMatchedExtension(Pricing.class));
        } finally {
            tearDown();
        }
    }

    @Test
    public void testInterceptorsWrapTheCallInRegistrationOrder() throws Exception {
        context.registerInterceptor(invocation -> {
            events.add("outer before");
            try {
                return invocation.proceed();
            } finally {
                events.add("outer after");
            }
        });
        context.registerInterceptor(invocation -> {
            events.add("inner before");
            try {
                return invocation.proceed();
            } finally {
                events.add("inner after");
            }
        });
        try {
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));

            assertEquals(List.of("outer before", "inner before", "inner after", "outer after"), events);
        } finally {
            tearDown();
        }
    }

    @Test
    public void testInterceptorCanChangeTheOutcome() throws Exception {
        context.registerInterceptor(invocation -> ((String) invocation.proceed()).toUpperCase());
        try {
            assertEquals("RETAIL PRICING", context.invoke(Pricing.class, Pricing::price));
        } finally {
            tearDown();
        }
    }

    @Test
    public void testInterceptorCanAnswerWithoutCallingTheImplementation() throws Exception {
        context.registerInterceptor(invocation -> "cached");
        try {
            assertEquals("cached", context.invoke(Pricing.class, Pricing::price));
        } finally {
            tearDown();
        }
    }

    @Test
    public void testInterceptorSeesTheCallAndCanChangeTheArguments() throws Exception {
        List<ExtensionInvocation> seen = new ArrayList<>();
        context.registerInterceptor(invocation -> {
            seen.add(invocation);
            if (invocation.method().getName().equals("discounted")) {
                invocation.arguments()[0] = 20;
            }
            return invocation.proceed();
        });
        try {
            assertEquals("retail pricing -20%", context.invoke(Pricing.class, e -> e.discounted(10)));
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));

            ExtensionInvocation discounted = seen.get(0);
            assertSame(Pricing.class, discounted.extensionPoint());
            assertEquals("retail", discounted.implementationCode());
            assertSame(retail, discounted.implementation());
            assertEquals("discounted", discounted.method().getName());
            assertArrayEquals(new Object[]{20}, discounted.arguments());

            assertArrayEquals(new Object[0], seen.get(1).arguments(), "never null, even without parameters");
        } finally {
            tearDown();
        }
    }

    @Test
    public void testImplementationFailureReachesTheInterceptorAndTheCallerUnchanged() throws Exception {
        // the default implementation of Shipping is served by the "default" code
        ShopBusiness failing = new ShopBusiness("failing", r -> "failing".equals(r.name), 0, List.of(Shipping.class), List.of()) {
            @Override
            public String ship() {
                throw new IllegalStateException("no courier");
            }
        };
        context.registerBusiness(failing);
        List<Throwable> seen = new ArrayList<>();
        context.registerInterceptor(invocation -> {
            try {
                return invocation.proceed();
            } catch (Throwable t) {
                seen.add(t);
                throw t;
            }
        });
        context.initSession(new Req("failing"));
        try {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> context.invoke(Shipping.class, Shipping::ship));
            assertEquals("no courier", e.getMessage());
            assertEquals(1, seen.size());
            assertSame(e, seen.get(0), "the interceptor saw the very exception the caller gets: nothing wrapped it");
        } finally {
            tearDown();
        }
    }

    @Test
    public void testAllMatchedExtensionsAreWrappedToo() throws Exception {
        context.registerInterceptor(invocation -> {
            events.add(invocation.implementationCode());
            return invocation.proceed();
        });
        try {
            List<String> prices = context.invokeAll(Pricing.class, Pricing::price);

            assertEquals(List.of("retail pricing", "default pricing"), prices);
            assertEquals(List.of("retail", "system.extension.point.default.implementation"), events);
        } finally {
            tearDown();
        }
    }

    @Test
    public void testProxiesBehindExtensionInjectPassThroughInterceptors() throws Exception {
        context.registerInterceptor(invocation -> {
            events.add("intercepted " + invocation.method().getName());
            return invocation.proceed();
        });
        Pricing injected = new FirstMatchedExtPointProxyFactory<>(Pricing.class, context).getProxy();
        try {
            assertEquals("retail pricing", injected.price());
            assertEquals(List.of("intercepted price"), events);
        } finally {
            tearDown();
        }
    }

    @Test
    public void testObjectMethodsAreNotExtensionCalls() throws Exception {
        context.registerInterceptor(invocation -> {
            events.add(invocation.method().getName());
            return invocation.proceed();
        });
        try {
            Pricing wrapped = context.getFirstMatchedExtension(Pricing.class);
            assertNotSame(retail, wrapped, "what is handed out is the wrapper");

            wrapped.toString();
            wrapped.hashCode();

            assertTrue(events.isEmpty(), "toString and hashCode are not intercepted: " + events);
        } finally {
            tearDown();
        }
    }

    @Test
    public void testInterceptorRegisteredLaterAppliesFromThenOn() throws Exception {
        try {
            Pricing before = context.getFirstMatchedExtension(Pricing.class);
            assertSame(retail, before);
            before.price();
            assertTrue(events.isEmpty());

            context.registerInterceptor(invocation -> {
                events.add("seen");
                return invocation.proceed();
            });
            context.getFirstMatchedExtension(Pricing.class).price();

            assertEquals(List.of("seen"), events);
        } finally {
            tearDown();
        }
    }

    @Test
    public void testWrapperIsBuiltOncePerImplementation() throws Exception {
        context.registerInterceptor(invocation -> invocation.proceed());
        try {
            assertSame(context.getFirstMatchedExtension(Pricing.class), context.getFirstMatchedExtension(Pricing.class),
                    "nothing is allocated per call");
        } finally {
            tearDown();
        }
    }

    @Test
    public void testNullInterceptorIsRejected() {
        RegisterParamException e = assertThrows(RegisterParamException.class, () -> context.registerInterceptor(null));
        assertEquals("interceptor should not be null", e.getMessage());
        tearDown();
    }
}
