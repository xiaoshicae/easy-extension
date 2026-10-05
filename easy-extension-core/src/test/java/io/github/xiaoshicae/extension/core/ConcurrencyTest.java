package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.Fixtures.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("concurrency")
public class ConcurrencyTest {

    private static final int THREADS = 16;
    private static final int ITERATIONS = 2000;

    @Test
    public void testManyThreadsBindDifferentParametersWithoutSeeingEachOther() throws Exception {
        ExtensionContext<Param> context = Fixtures.base().business(new RetailBusiness()).business(new FreshBusiness()).build();
        Pay pay = context.proxy(Pay.class);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final boolean retail = t % 2 == 0;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < ITERATIONS; i++) {
                        try (Binding ignored = context.bind(Param.of(retail ? "retail" : "fresh"))) {
                            assertEquals(retail ? "retail-pay" : "fresh-pay", pay.pay());
                            // a nested binding switches identity and gives it back
                            try (Binding inner = context.bind(Param.of(retail ? "fresh" : "retail"))) {
                                assertEquals(retail ? "fresh-pay" : "retail-pay", pay.pay());
                            }
                            assertEquals(retail ? "retail-pay" : "fresh-pay", pay.pay());
                        }
                    }
                    assertThrows(RuntimeException.class, pay::pay);
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
