package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.ResolutionException;
import io.github.xiaoshicae.extension.core.exception.ResolutionException.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static io.github.xiaoshicae.extension.core.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The shortcuts around {@link Binding} on one thread: run with a binding, ask whether one exists, read a resolution.
 * Handing a binding to other threads: {@link BindingHandOverTest}.
 */
public class BindingConvenienceTest {
    private ExtensionContext<Param> context;

    @BeforeEach
    public void setUp() {
        context = Fixtures.base().business(new RetailBusiness()).business(new FreshBusiness()).build();
    }

    private void assertNothingBound() {
        assertFalse(context.isBound());
        assertEquals(Reason.NO_BINDING, assertThrows(ResolutionException.class, () -> context.current()).reason());
    }

    private static void assertNullArgument(String name, Executable call) {
        assertEquals(name, assertThrows(NullPointerException.class, call).getMessage());
    }

    // ---- runWith / callWith

    @Test
    public void testRunWithBindsOnlyWhileTheBodyRuns() {
        List<String> seen = new ArrayList<>();

        context.runWith(Param.of("retail"), () -> {
            assertTrue(context.isBound());
            seen.add(context.first(Pay.class).pay());
        });

        assertEquals(List.of("retail-pay"), seen);
        assertNothingBound();
    }

    @Test
    public void testCallWithReturnsWhatTheBodyReturns() {
        assertEquals("fresh-pay", context.callWith(Param.of("fresh"), () -> context.first(Pay.class).pay()));
        assertNothingBound();
    }

    @Test
    public void testTheBindingEndsWhenTheBodyThrows() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> context.runWith(Param.of("retail"), () -> {
            throw failure;
        }));

        assertSame(failure, e);
        assertNothingBound();
    }

    @Test
    public void testRunWithNestsAndRestoresTheOuterBinding() {
        context.runWith(Param.of("retail"), () -> {
            assertEquals("retail-pay", context.first(Pay.class).pay());

            // act as another business for a while
            context.runWith(Param.of("fresh"), () -> assertEquals("fresh-pay", context.first(Pay.class).pay()));

            assertEquals("retail-pay", context.first(Pay.class).pay());
        });
        assertNothingBound();
    }

    @Test
    public void testRunWithAnExistingResolution() {
        Resolution resolution = context.resolve(Param.of("fresh"));

        String result = context.callWith(resolution, () -> context.first(Pay.class).pay());

        assertEquals("fresh-pay", result);
        context.runWith(resolution, () -> assertSame(resolution, context.current()));
        assertNothingBound();
    }

    @Test
    public void testStrictModeFailureBindsNothing() {
        ResolutionException e = assertThrows(ResolutionException.class,
                () -> context.runWith(Param.of("nobody"), () -> fail("must not run")));

        assertEquals(Reason.NO_BUSINESS_MATCHED, e.reason());
        assertEquals("no business matched", e.getMessage());
        assertNothingBound();
    }

    @Test
    public void testBodiesMayNotBeNull() {
        assertNullArgument("body", () -> context.runWith(Param.of("retail"), null));
        assertNullArgument("body", () -> context.callWith(Param.of("retail"), null));
        assertNullArgument("task", () -> context.wrap(null));
        assertNullArgument("delegate", () -> context.executor(null));
    }

    // ---- isBound

    @Test
    public void testIsBoundFollowsTheBinding() {
        assertFalse(context.isBound());
        try (Binding ignored = context.bind(Param.of("retail"))) {
            assertTrue(context.isBound());
        }
        assertFalse(context.isBound());
    }

    // ---- wrap

    @Test
    public void testWrapWithoutABindingReturnsTheTaskUnchanged() {
        Runnable task = () -> { };

        assertSame(task, context.wrap(task));
    }

    @Test
    public void testWrapRunInTheCallingThreadNestsHarmlessly() {
        Runnable[] wrapped = new Runnable[1];
        context.runWith(Param.of("retail"), () -> {
            wrapped[0] = context.wrap(() -> assertEquals("retail-pay", context.first(Pay.class).pay()));
            // e.g. a caller-runs rejection policy: the task runs right here, inside the same binding
            wrapped[0].run();
            assertTrue(context.isBound());
        });
        assertNothingBound();
    }

    // ---- diagnostics

    @Test
    public void testResolutionToStringIsReadable() {
        Resolution resolution = context.resolve(Param.of("fresh"));

        String text = resolution.toString();

        assertTrue(text.startsWith("Resolution[business=biz.fresh, chain=["), text);
        assertTrue(text.contains("business(biz.fresh"), text);
    }
}
