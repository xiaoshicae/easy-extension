package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry managers are read without a lock while registrations may still be going on.
 * Readers must always see a consistent snapshot: no torn lists, nothing disappearing, nothing half registered.
 */
@Tag("concurrency")
public class RegistrySnapshotConcurrencyTest {

    @Test
    public void testReadersSeeConsistentSnapshotsWhileBusinessesAreRegistered() throws Exception {
        int registrations = 300;
        int readers = 4;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean writing = new AtomicBoolean(true);
        ConcurrentLinkedQueue<String> problems = new ConcurrentLinkedQueue<>();

        pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < registrations; i++) {
                    manager.registerBusiness(new CodedBusiness("biz-" + i));
                }
            } catch (Throwable t) {
                problems.add("writer: " + t);
            } finally {
                writing.set(false);
            }
        });

        for (int r = 0; r < readers; r++) {
            pool.submit(() -> {
                try {
                    start.await();
                    int lastSize = 0;
                    while (writing.get()) {
                        List<IBusiness<Object>> snapshot = manager.listAllBusinesses();
                        if (snapshot.size() < lastSize) {
                            problems.add("a later read saw fewer businesses: " + snapshot.size() + " < " + lastSize);
                        }
                        lastSize = snapshot.size();

                        Set<String> codes = new HashSet<>();
                        for (int i = 0; i < snapshot.size(); i++) {
                            IBusiness<Object> business = snapshot.get(i);
                            if (!("biz-" + i).equals(business.code())) {
                                problems.add("registration order broken at " + i + ": " + business.code());
                            }
                            if (!codes.add(business.code())) {
                                problems.add("duplicate in snapshot: " + business.code());
                            }
                            // whatever a list contains, a lookup (made later) finds
                            if (manager.findBusiness(business.code()) != business) {
                                problems.add("listed but not found: " + business.code());
                            }
                        }
                    }
                } catch (Throwable t) {
                    problems.add("reader: " + t);
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "pool did not finish in time");
        assertTrue(problems.isEmpty(), "inconsistent reads: " + problems);
        assertEquals(registrations, manager.listAllBusinesses().size());
    }

    private static final class CodedBusiness extends AbstractBusiness<Object> {
        private final String code;

        private CodedBusiness(String code) {
            this.code = code;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public boolean match(Object param) {
            return false;
        }

        @Override
        public Integer priority() {
            return 0;
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            return List.of();
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of();
        }
    }
}
