package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.Fixtures.*;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class BindingTest {
    private ExtensionContext<Param> context;

    @BeforeEach
    public void setUp() {
        context = Fixtures.base().business(new RetailBusiness()).business(new FreshBusiness()).build();
    }

    @Test
    public void testNothingBoundByDefault() {
        ResolutionException e = assertThrows(ResolutionException.class, () -> context.first(Pay.class));
        assertEquals(Reason.NO_BINDING, e.reason());
        assertEquals("no resolution is bound to thread [" + Thread.currentThread().getName()
                + "], bind one first: try (Binding b = context.bind(param)) { ... }", e.getMessage());
    }

    @Test
    public void testBindMakesTheResolutionCurrentUntilClosed() {
        try (Binding binding = context.bind(Param.of("retail"))) {
            assertSame(binding.resolution(), context.current());
            assertEquals("retail-pay", context.first(Pay.class).pay());
            assertEquals("retail-pay", context.invoke(Pay.class, Pay::pay));
        }
        assertEquals(Reason.NO_BINDING, assertThrows(ResolutionException.class, () -> context.current()).reason());
    }

    @Test
    public void testResolveDoesNotBindAnything() {
        context.resolve(Param.of("retail"));
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testBindingsNestAndClosingRestoresThePreviousOne() {
        try (Binding outer = context.bind(Param.of("retail"))) {
            try (Binding inner = context.bind(Param.of("fresh"))) {
                assertEquals("fresh-pay", context.first(Pay.class).pay());
                assertSame(inner.resolution(), context.current());
            }
            assertSame(outer.resolution(), context.current());
            assertEquals("retail-pay", context.first(Pay.class).pay());
        }
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testCloseIsIdempotent() {
        Binding outer = context.bind(Param.of("retail"));
        Binding inner = context.bind(Param.of("fresh"));
        inner.close();
        inner.close();
        assertSame(outer.resolution(), context.current());
        outer.close();
        outer.close();
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testClosingAnOuterBindingDropsTheOnesOpenedAfterIt() {
        Binding outer = context.bind(Param.of("retail"));
        context.bind(Param.of("fresh"));
        outer.close();
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testClearDropsEveryBindingAndLateClosesAreHarmless() {
        Binding first = context.bind(Param.of("retail"));
        Binding second = context.bind(Param.of("fresh"));
        context.clear();
        assertThrows(ResolutionException.class, () -> context.current());

        Binding fresh = context.bind(Param.of("fresh"));
        // closing bindings from before clear() must not touch the new one
        second.close();
        first.close();
        assertSame(fresh.resolution(), context.current());
        fresh.close();
    }

    @Test
    public void testBindingsAreThreadLocal() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Binding ignored = context.bind(Param.of("retail"))) {
            ResolutionException other = executor.submit(() -> assertThrows(ResolutionException.class, () -> context.current())).get();
            assertEquals(Reason.NO_BINDING, other.reason());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAResolutionCanContinueOnAnotherThread() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Resolution resolution = context.resolve(Param.of("fresh"));
            String pay = executor.submit(() -> {
                try (Binding ignored = context.bind(resolution)) {
                    return context.first(Pay.class).pay();
                }
            }).get();
            assertEquals("fresh-pay", pay);
            // the pool thread is clean again afterwards
            assertEquals(Reason.NO_BINDING, executor.submit(() ->
                    assertThrows(ResolutionException.class, () -> context.current())).get().reason());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSeveralContextsDoNotShareBindings() {
        ExtensionContext<Param> other = Fixtures.base().business(new RetailBusiness()).build();
        try (Binding ignored = context.bind(Param.of("retail"))) {
            assertThrows(ResolutionException.class, other::current);
        }
    }

    @Test
    public void testResolutionFailureBindsNothing() {
        assertThrows(ResolutionException.class, () -> context.bind(Param.of("unknown")));
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testABindingMustBeClosedByTheThreadThatOpenedIt() throws Exception {
        Binding binding = context.bind(Param.of("retail"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            IllegalStateException e = executor.submit(() -> assertThrows(IllegalStateException.class, binding::close)).get();
            assertTrue(e.getMessage().startsWith("a Binding must be closed by the thread that opened it ["), e.getMessage());
            // still bound, and the owner can close it
            assertSame(binding.resolution(), context.current());
        } finally {
            executor.shutdown();
            binding.close();
        }
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testOnlyResolutionsOfThisContextCanBeBound() {
        Resolution foreign = Fixtures.base().business(new RetailBusiness()).build().resolve(Param.of("retail"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> context.bind(foreign));
        assertEquals("the resolution was not created by this context", e.getMessage());
        assertThrows(ResolutionException.class, () -> context.current());
    }

    @Test
    public void testBindingsWorkOnVirtualThreads() throws Exception {
        Pay pay = context.proxy(Pay.class);
        String[] result = new String[2];
        Thread thread = Thread.ofVirtual().start(() -> {
            try (Binding ignored = context.bind(Param.of("fresh"))) {
                result[0] = pay.pay();
            }
            result[1] = assertThrows(ResolutionException.class, pay::pay).getMessage();
        });
        thread.join();

        assertEquals("fresh-pay", result[0]);
        // a virtual thread has no name: the message says which one it is
        assertTrue(result[1].startsWith("no resolution is bound to thread [virtual-"), result[1]);
    }

    @Test
    public void testClosingAnAlreadyClosedBindingFromAnotherThreadIsANoOp() throws Exception {
        Binding binding = context.bind(Param.of("retail"));
        binding.close();

        Throwable[] failure = new Throwable[1];
        Thread other = new Thread(() -> {
            try {
                binding.close();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        other.start();
        other.join();

        assertNull(failure[0]);
    }
}
