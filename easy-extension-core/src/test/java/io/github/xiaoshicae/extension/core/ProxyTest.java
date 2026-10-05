package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What a caller sees through {@code proxy(...)} / {@code proxyAll(...)}: the implementation's own exceptions, and
 * bindings that decide who answers.
 */
public class ProxyTest {

    @ExtensionPoint
    public interface Risky {
        String run();

        void fail() throws IOException;
    }

    @DefaultImplementation
    public static class DefaultRisky implements Risky {
        @Override
        public String run() {
            return "default";
        }

        @Override
        public void fail() throws IOException {
            throw new IOException("default io");
        }
    }

    @Business(code = "biz.risky")
    public static class RiskyBusiness implements Matcher<Param>, Risky {
        @Override
        public boolean match(Param param) {
            return "risky".equals(param.tenant());
        }

        @Override
        public String run() {
            throw new IllegalStateException("boom");
        }

        @Override
        public void fail() throws IOException {
            throw new IOException("business io");
        }
    }

    private final ExtensionContext<Param> context = Fixtures.base().extensionPoint(Risky.class)
            .defaultImplementation(new DefaultRisky()).business(new RiskyBusiness())
            .business(new RetailBusiness()).business(new FreshBusiness()).build();

    @Test
    public void testTheProxyAnswersFromTheCurrentBinding() {
        Pay pay = context.proxy(Pay.class);
        try (Binding ignored = context.bind(Param.of("retail"))) {
            assertEquals("retail-pay", pay.pay());
        }
        try (Binding ignored = context.bind(Param.of("fresh"))) {
            assertEquals("fresh-pay", pay.pay());
        }
    }

    @Test
    public void testNestedBindingSwitchesWhoAnswersTemporarily() {
        Pay pay = context.proxy(Pay.class);
        try (Binding outer = context.bind(Param.of("retail"))) {
            try (Binding inner = context.bind(Param.of("fresh"))) {
                assertEquals("fresh-pay", pay.pay());
            }
            assertEquals("retail-pay", pay.pay());
        }
    }

    @Test
    public void testExceptionsOfTheImplementationComeOutAsTheyAre() {
        Risky risky = context.proxy(Risky.class);
        try (Binding ignored = context.bind(Param.of("risky"))) {
            IllegalStateException unchecked = assertThrows(IllegalStateException.class, risky::run);
            assertEquals("boom", unchecked.getMessage());
            IOException declared = assertThrows(IOException.class, risky::fail);
            assertEquals("business io", declared.getMessage());
        }
    }

    @Test
    public void testCallingWithoutABindingIsAResolutionException() {
        Pay pay = context.proxy(Pay.class);
        ResolutionException e = assertThrows(ResolutionException.class, pay::pay);
        assertEquals(Reason.NO_BINDING, e.reason());
    }

    @Test
    public void testObjectMethodsNeverNeedABinding() {
        Pay pay = context.proxy(Pay.class);
        assertEquals("ExtensionProxy[" + Pay.class.getName() + "]", pay.toString());
        assertEquals(System.identityHashCode(pay), pay.hashCode());
        assertEquals(pay, pay);
        assertNotEquals(pay, context.proxy(Pay.class));
    }

    @Test
    public void testOnlyRegisteredExtensionPointsCanBeProxied() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> context.proxy(Runnable.class));
        assertEquals("extension point [java.lang.Runnable] is not registered", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> context.proxyAll(Runnable.class));
    }

    @Test
    public void testProxyAllIsAListViewOfTheCurrentBinding() {
        List<Pay> all = context.proxyAll(Pay.class);

        // no binding: only toString is safe, e.g. for logging
        assertEquals("ExtensionProxyList[" + Pay.class.getName() + "]", all.toString());
        ResolutionException e = assertThrows(ResolutionException.class, all::size);
        assertEquals(Reason.NO_BINDING, e.reason());

        try (Binding ignored = context.bind(Param.of("fresh", "alipay"))) {
            assertEquals(3, all.size());
            assertEquals(List.of("ability-alipay", "fresh-pay", "default-pay"), all.stream().map(Pay::pay).toList());
            assertEquals("fresh-pay", all.get(1).pay());
            // it behaves like the list it reflects
            assertEquals(context.all(Pay.class), all);
            assertEquals(context.all(Pay.class).hashCode(), all.hashCode());
            assertThrows(UnsupportedOperationException.class, () -> all.add(new DefaultPay()));
        }
        try (Binding ignored = context.bind(Param.of("retail"))) {
            assertEquals(2, all.size());
        }
    }
}
