package io.github.xiaoshicae.extension.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.xiaoshicae.extension.core.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Handing a binding over to another thread with {@code wrap} and {@code executor}, and what must not leak between threads.
 */
@Tag("concurrency")
public class BindingHandOverTest {
    private ExtensionContext<Param> context;

    @BeforeEach
    public void setUp() {
        context = Fixtures.base().business(new RetailBusiness()).business(new FreshBusiness()).build();
    }

    @Test
    public void testIsBoundIsPerThread() throws Exception {
        boolean[] otherThread = new boolean[1];

        try (Binding ignored = context.bind(Param.of("retail"))) {
            Thread thread = new Thread(() -> otherThread[0] = context.isBound());
            thread.start();
            thread.join();
            assertTrue(context.isBound());
        }

        assertFalse(otherThread[0]);
    }

    @Test
    public void testWrapCarriesTheBindingToAnotherThread() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        AtomicReference<Boolean> boundAfter = new AtomicReference<>();
        Runnable wrapped;
        try (Binding ignored = context.bind(Param.of("fresh"))) {
            wrapped = context.wrap(() -> seen.set(context.first(Pay.class).pay()));
        }

        // the original call is over, the task still sees what was bound when it was wrapped
        Thread thread = new Thread(() -> {
            wrapped.run();
            boundAfter.set(context.isBound());
        });
        thread.start();
        thread.join();

        assertEquals("fresh-pay", seen.get());
        assertEquals(Boolean.FALSE, boundAfter.get());
    }

    @Test
    public void testWrappedTaskThatThrowsStillUnbinds() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Boolean> boundAfter = new AtomicReference<>();
        Runnable wrapped;
        try (Binding ignored = context.bind(Param.of("retail"))) {
            wrapped = context.wrap(() -> {
                throw new IllegalStateException("boom");
            });
        }

        Thread thread = new Thread(() -> {
            try {
                wrapped.run();
            } catch (Throwable t) {
                failure.set(t);
            }
            boundAfter.set(context.isBound());
        });
        thread.start();
        thread.join();

        assertEquals("boom", failure.get().getMessage());
        assertEquals(Boolean.FALSE, boundAfter.get());
    }

    @Test
    public void testExecutorCapturesTheBindingOfTheSubmittingThread() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var bound = context.executor(pool);
            CompletableFuture<String> retail = new CompletableFuture<>();
            CompletableFuture<String> fresh = new CompletableFuture<>();

            // two submitters with different identities share the same pool threads
            context.runWith(Param.of("retail"), () -> bound.execute(() -> retail.complete(context.first(Pay.class).pay())));
            context.runWith(Param.of("fresh"), () -> bound.execute(() -> fresh.complete(context.first(Pay.class).pay())));

            assertEquals("retail-pay", retail.get(5, TimeUnit.SECONDS));
            assertEquals("fresh-pay", fresh.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testExecutorWithCompletableFuture() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            CompletableFuture<String> future = context.callWith(Param.of("retail"),
                    () -> CompletableFuture.supplyAsync(() -> context.first(Pay.class).pay(), context.executor(pool)));

            assertEquals("retail-pay", future.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testExecutorDoesNotLeakABindingToTheNextTask() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            var bound = context.executor(pool);
            context.runWith(Param.of("retail"), () -> bound.execute(() -> { }));

            // submitted with nothing bound: the (reused) pool thread must not still carry the previous task's binding
            CompletableFuture<Boolean> seen = new CompletableFuture<>();
            bound.execute(() -> seen.complete(context.isBound()));

            assertEquals(Boolean.FALSE, seen.get(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testWrapHandsTheBindingToAVirtualThread() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> seen = new AtomicReference<>();

        context.runWith(Param.of("retail"), () -> Thread.ofVirtual().start(context.wrap(() -> {
            seen.set(context.first(Pay.class).pay());
            done.countDown();
        })));

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals("retail-pay", seen.get());
    }
}
