package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.session.ResolvedChain;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A business is registered while requests are being served. Whatever moment a request arrives at, it must be
 * resolved to a business that the registry can serve, and the chain must carry the version of a registry that
 * contains what it names.
 * <p>
 * The registration is stepped through: it is held up inside every call it makes on the business (which is every
 * place it may be half way), and a request is made at each of those moments.
 * </p>
 */
@Tag("concurrency")
public class RegistrationVisibilityTest {

    /** A business that hands the registering thread over to the test inside every call the registration makes on it. */
    private static final class SteppedBusiness extends ShopBusiness {
        private final BlockingQueue<String> events = new LinkedBlockingQueue<>();
        private final Semaphore proceed = new Semaphore(0);
        private volatile Thread registrar;

        private SteppedBusiness() {
            super("late", r -> "late".equals(r.name), 0, List.of(Pricing.class, Tax.class), List.of());
        }

        private void step(String call) {
            if (Thread.currentThread() != registrar) {
                return;
            }
            events.add(call);
            try {
                if (!proceed.tryAcquire(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("registration was never released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String code() {
            step("code()");
            return super.code();
        }

        @Override
        public Integer priority() {
            step("priority()");
            return super.priority();
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            step("usedAbilities()");
            return super.usedAbilities();
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            step("implementExtensionPoints()");
            return super.implementExtensionPoints();
        }
    }

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(ShopBusiness.named("retail", Pricing.class, Tax.class));
        return context;
    }

    /** Registers {@code late} on another thread and calls {@code atEveryStep} while that thread is inside the registration. */
    private static void registerStepByStep(DefaultExtensionContext<Req> context, SteppedBusiness late, Consumer<String> atEveryStep) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Void> registration = pool.submit(() -> {
                late.registrar = Thread.currentThread();
                try {
                    context.registerBusiness(late);
                } finally {
                    late.events.add("done");
                }
                return null;
            });
            while (true) {
                String event = late.events.poll(30, TimeUnit.SECONDS);
                assertNotNull(event, "the registration did not make progress");
                if (event.equals("done")) {
                    break;
                }
                try {
                    atEveryStep.accept(event);
                } finally {
                    late.proceed.release();
                }
            }
            registration.get(30, TimeUnit.SECONDS);
        } finally {
            late.proceed.release(1000);
            pool.shutdownNow();
        }
    }

    @Test
    public void testARequestIsNeverResolvedToABusinessThatIsNotServedYet() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        List<String> problems = new ArrayList<>();

        registerStepByStep(context, new SteppedBusiness(), step -> {
            try {
                context.initSession(new Req("late"));
                try {
                    boolean resolvedToLate = context.currentChain().codes().contains("late");
                    String price = context.invoke(Pricing.class, Pricing::price);
                    // either the business is not there yet (an unknown business, served by the defaults) or it serves
                    String expected = resolvedToLate ? "late pricing" : "default pricing";
                    if (!expected.equals(price)) {
                        problems.add("inside " + step + ": resolved to late=" + resolvedToLate + " but served '" + price + "'");
                    }
                } finally {
                    context.removeSession();
                }
            } catch (Exception e) {
                problems.add("inside " + step + ": " + e);
            }
        });

        assertTrue(problems.isEmpty(), problems.toString());
        context.initSession(new Req("late"));
        try {
            assertEquals("late pricing", context.invoke(Pricing.class, Pricing::price), "once registered, it serves");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testAChainCarriesTheVersionOfARegistryThatContainsWhatItNames() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        String versionBefore = context.registryVersion();
        List<String> problems = new ArrayList<>();

        registerStepByStep(context, new SteppedBusiness(), step -> {
            try {
                ResolvedChain chain = context.resolve(new Req("late"));
                if (chain.codes().contains("late") && chain.registryVersion().equals(versionBefore)) {
                    problems.add("inside " + step + ": the chain names 'late' but is labelled with the registry that did not have it");
                }
            } catch (Exception e) {
                problems.add("inside " + step + ": " + e);
            }
        });

        assertTrue(problems.isEmpty(), problems.toString());
    }
}
