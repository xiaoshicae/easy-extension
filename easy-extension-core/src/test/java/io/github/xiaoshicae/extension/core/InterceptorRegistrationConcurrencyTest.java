package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interceptors may be registered while requests are being served. Once a registration has returned, whatever the
 * context hands out must run through that interceptor too: a wrapper built for the previous interceptors by a call
 * that was in flight during the registration must not stay behind.
 */
@Tag("concurrency")
public class InterceptorRegistrationConcurrencyTest {

    /** The interceptors that ran for the call being made on this thread, in the order they ran. */
    private static final ThreadLocal<List<Integer>> RAN = ThreadLocal.withInitial(ArrayList::new);

    @Test
    public void testLookupAfterRegistrationRunsThroughEveryInterceptorRegisteredSoFar() throws Exception {
        // the window the race needs is a few instructions wide, so one round only hits it now and then: repeat
        for (int round = 0; round < 25; round++) {
            registerInterceptorsWhileReadersLookUp(200, 4);
        }
    }

    private static void registerInterceptorsWhileReadersLookUp(int interceptors, int readers) throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(ShopBusiness.named("retail", Pricing.class, Tax.class));

        ExecutorService pool = Executors.newFixedThreadPool(readers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean registering = new AtomicBoolean(true);
        ConcurrentLinkedQueue<String> problems = new ConcurrentLinkedQueue<>();

        for (int r = 0; r < readers; r++) {
            pool.submit(() -> {
                try {
                    start.await();
                    context.initSession(new Req("retail"));
                    try {
                        while (registering.get()) {
                            RAN.get().clear();
                            context.getFirstMatchedExtension(Pricing.class).price();
                            // registration is going on, so any number of interceptors may be in place, but always
                            // the first ones, each once, in the order they were registered
                            List<Integer> ran = RAN.get();
                            for (int k = 0; k < ran.size(); k++) {
                                if (ran.get(k) != k) {
                                    problems.add("interceptors ran out of order: " + ran);
                                    break;
                                }
                            }
                        }
                    } finally {
                        context.removeSession();
                    }
                } catch (Throwable t) {
                    problems.add("reader: " + t);
                }
            });
        }

        try {
            context.initSession(new Req("retail"));
            start.countDown();
            for (int i = 0; i < interceptors; i++) {
                int index = i;
                context.registerInterceptor(invocation -> {
                    RAN.get().add(index);
                    return invocation.proceed();
                });

                RAN.get().clear();
                assertEquals("retail pricing", context.getFirstMatchedExtension(Pricing.class).price());
                assertEquals(IntStream.rangeClosed(0, i).boxed().toList(), RAN.get(),
                        "a lookup made after interceptor " + i + " was registered must run through all of them");
            }
        } finally {
            registering.set(false);
            start.countDown();
            context.removeSession();
            RAN.remove();
            pool.shutdown();
        }
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "pool did not finish in time");
        assertTrue(problems.isEmpty(), "inconsistent interception: " + problems);
    }
}
