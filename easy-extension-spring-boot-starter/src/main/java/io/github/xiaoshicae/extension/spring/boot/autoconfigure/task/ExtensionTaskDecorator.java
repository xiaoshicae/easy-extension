package io.github.xiaoshicae.extension.spring.boot.autoconfigure.task;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import org.springframework.core.task.TaskDecorator;

/**
 * Lets a task run with the business of the thread that submitted it, which is not the case by default: a binding
 * belongs to one thread. Set it on a Spring task executor:
 * <pre>{@code
 * executor.setTaskDecorator(new ExtensionTaskDecorator(context));
 * }</pre>
 * or let the starter do it for the executors Spring Boot configures ({@code @Async}, {@code applicationTaskExecutor}) with
 * {@code easy-extension.async-propagation=true}. The task sees the resolution as it was at submission time, even if the
 * request has completed by the time it runs. Nothing is carried over when nothing is bound at submission.
 */
public class ExtensionTaskDecorator implements TaskDecorator {
    private final ExtensionContext<?> context;

    /**
     * @param context the context whose binding is handed over
     */
    public ExtensionTaskDecorator(ExtensionContext<?> context) {
        this.context = context;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return context.wrap(runnable);
    }
}
