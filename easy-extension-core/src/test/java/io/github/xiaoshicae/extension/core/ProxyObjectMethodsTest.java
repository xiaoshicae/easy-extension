package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code toString()}, {@code hashCode()} and {@code equals()} of the framework's proxies are for logs, debuggers,
 * {@code Lombok}-generated methods and collections. They must not need a session (an injected proxy used to throw
 * "session not init" from all three) and {@code equals} must be reflexive.
 */
public class ProxyObjectMethodsTest {

    @Business(code = "biz.annotated")
    public static class AnnotatedBusiness implements Matcher<Req>, Pricing {
        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public String price() {
            return "annotated";
        }

        @Override
        public String discounted(int percent) {
            return "annotated";
        }
    }

    /** Stands for a Spring bean with {@code @ExtensionInject Pricing pricing} and Lombok's {@code @ToString}. */
    static final class Decorating extends ShopBusiness {
        Pricing pricing;

        Decorating() {
            super("decorating", r -> true, 0, List.of(Pricing.class), List.of());
        }

        @Override
        public String toString() {
            return "Decorating(pricing=" + pricing + ")";
        }
    }

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(new ShopBusiness("retail", r -> true, 0, List.of(Pricing.class), List.of()));
        return context;
    }

    @Test
    public void testInjectedProxyHasObjectMethodsWithoutASession() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        Pricing injected = new FirstMatchedExtPointProxyFactory<>(Pricing.class, context).getProxy();

        assertTrue(injected.toString().contains("Pricing"), "says what it is: " + injected);
        assertEquals(injected.hashCode(), injected.hashCode());
        assertTrue(injected.equals(injected), "reflexive");
        Set<Object> set = new HashSet<>();
        set.add(injected);
        assertTrue(set.contains(injected), "usable as a key");
    }

    @Test
    public void testInjectedProxyIsEqualToItselfInASessionToo() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        Pricing injected = new FirstMatchedExtPointProxyFactory<>(Pricing.class, context).getProxy();
        context.initSession(new Req("retail"));
        try {
            assertTrue(injected.equals(injected), "reflexive");
            assertTrue(List.of(injected).contains(injected));
            assertEquals("retail pricing", injected.price(), "calls are still routed");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testPrintingAnImplementationThatHoldsTheProxyOfItsOwnExtensionPoint() throws Exception {
        // the business prints its fields, one of which is the proxy, which used to print the first implementation
        // in the chain: the business. StackOverflowError.
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        Decorating decorating = new Decorating();
        decorating.pricing = new FirstMatchedExtPointProxyFactory<>(Pricing.class, context).getProxy();
        context.registerBusiness(decorating);

        context.initSession(new Req("anybody"));
        try {
            String printed = decorating.toString();
            assertTrue(printed.startsWith("Decorating(pricing="), printed);
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testInjectedListProxyHasObjectMethodsWithoutASession() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        List<Pricing> injected = new AllMatchedExtPointProxyFactory<>(Pricing.class, context).getProxy();

        assertTrue(injected.toString().contains("Pricing"), "says what it is: " + injected);
        assertEquals(injected.hashCode(), injected.hashCode());
        assertTrue(injected.equals(injected), "reflexive");
    }

    @Test
    public void testRegisteredProxyIsEqualToItself() throws Exception {
        IBusiness<Req> proxy = AnnProxyConvertUtils.convertAnnBusinessToProxy(new AnnotatedBusiness());

        assertTrue(proxy.equals(proxy), "reflexive");
        assertTrue(List.of(proxy).contains(proxy), "found in a list that holds it");
        assertEquals(1, new HashSet<>(List.of(proxy, proxy)).size());
    }

    @Test
    public void testInterceptedExtensionIsEqualToItself() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.registerInterceptor(invocation -> invocation.proceed());
        context.initSession(new Req("retail"));
        try {
            Pricing intercepted = context.getFirstMatchedExtension(Pricing.class);

            assertTrue(intercepted.equals(intercepted), "reflexive");
            assertTrue(List.of(intercepted).contains(intercepted));
        } finally {
            context.removeSession();
        }
    }
}
