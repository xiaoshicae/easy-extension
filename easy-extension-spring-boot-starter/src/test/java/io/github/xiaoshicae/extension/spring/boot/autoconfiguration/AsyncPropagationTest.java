package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.task.ExtensionTaskDecorator;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain.Param;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.DomainConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Handing the binding over to Spring task executors ({@code @Async}, {@code applicationTaskExecutor}): off by default, on
 * with {@code easy-extension.async-propagation=true}, and combined with the application's own {@code TaskDecorator}.
 */
@Tag("concurrency")
public class AsyncPropagationTest {
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EasyExtensionAutoConfiguration.class);

    @BeforeEach
    public void captureLogs() {
        logs.start();
        logger.setLevel(Level.DEBUG);
        logger.addAppender(logs);
    }

    @AfterEach
    public void releaseLogs() {
        logger.detachAppender(logs);
        logger.setLevel(null);
        logs.stop();
    }

    private List<String> messages() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private final ApplicationContextRunner asyncRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EasyExtensionAutoConfiguration.class, TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(DomainConfig.class)
            .withPropertyValues("easy-extension.allow-unknown-business=true");

    @Test
    public void testNoTaskDecoratorUnlessAsked() {
        asyncRunner.run(ctx -> assertTrue(ctx.getBeansOfType(ExtensionTaskDecorator.class).isEmpty()));
        assertTrue(messages().stream().noneMatch(m -> m.contains("async propagation is on")), messages().toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testByDefaultTasksDoNotInheritTheBinding() {
        asyncRunner.run(ctx -> {
            ExtensionContext<Param> context = ctx.getBean(ExtensionContext.class);
            ThreadPoolTaskExecutor executor = ctx.getBean(ThreadPoolTaskExecutor.class);

            boolean boundInTask = context.callWith(new Param("retail"), () -> {
                try {
                    return executor.submit(context::isBound).get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            assertFalse(boundInTask, "a binding belongs to one thread unless the hand-over is switched on");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAsyncPropagationFollowsTasksOntoTheSpringExecutor() {
        asyncRunner.withPropertyValues("easy-extension.async-propagation=true").run(ctx -> {
            ExtensionContext<Param> context = ctx.getBean(ExtensionContext.class);
            ThreadPoolTaskExecutor executor = ctx.getBean(ThreadPoolTaskExecutor.class);

            boolean boundInTask = context.callWith(new Param("retail"), () -> {
                try {
                    return executor.submit(() -> context.isBound() && "biz.retail".equals(context.current().trace().matchedBusinessCode()))
                            .get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            boolean boundWithoutBinding = executor.submit(context::isBound).get();

            assertTrue(boundInTask);
            assertFalse(boundWithoutBinding, "nothing bound at submission, nothing in the task");
            assertTrue(messages().stream().anyMatch(m -> m.contains("async propagation is on")), messages().toString());
        });
    }

    @Configuration
    static class OtherDecorator {
        static final AtomicInteger DECORATED = new AtomicInteger();

        @Bean
        TaskDecorator countingDecorator() {
            return runnable -> {
                DECORATED.incrementAndGet();
                return runnable;
            };
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testItCombinesWithTheApplicationsOwnTaskDecorator() {
        OtherDecorator.DECORATED.set(0);
        asyncRunner.withUserConfiguration(OtherDecorator.class).withPropertyValues("easy-extension.async-propagation=true").run(ctx -> {
            ExtensionContext<Param> context = ctx.getBean(ExtensionContext.class);
            ThreadPoolTaskExecutor executor = ctx.getBean(ThreadPoolTaskExecutor.class);

            boolean boundInTask = context.callWith(new Param("retail"), () -> {
                try {
                    return executor.submit(context::isBound).get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            assertTrue(boundInTask);
            assertEquals(1, OtherDecorator.DECORATED.get(), "the application's own decorator still runs");
        });
    }

    @Test
    public void testTaskDecoratorOnAnExecutorOfYourOwn() throws Exception {
        ExtensionContext<Param> context = ExtensionContext.<Param>builder().strict(false)
                .extensionPoint(Domain.Pay.class, Domain.Ship.class)
                .defaultImplementation(new Domain.DefaultPay())
                .defaultImplementation(new Domain.DefaultShip())
                .build();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(new ExtensionTaskDecorator(context));
        executor.initialize();
        try {
            boolean bound = context.callWith(new Param("anyone"), () -> {
                try {
                    return executor.submit(context::isBound).get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            assertTrue(bound);
        } finally {
            executor.shutdown();
        }
    }
}
