package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionProxies;
import io.github.xiaoshicae.extension.core.Resolution;
import io.github.xiaoshicae.extension.core.catalog.ExtensionCatalog;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

final class DefaultExtensionContext<T> implements ExtensionContext<T> {
    private final Registry<T> registry;
    private final Resolver<T> resolver;
    // per thread, per context; nested bindings form a stack
    private final ThreadLocal<Deque<DefaultBinding>> bindings = new ThreadLocal<>();

    DefaultExtensionContext(Registry<T> registry) {
        this.registry = registry;
        this.resolver = new Resolver<>(registry);
    }

    @Override
    public Resolution resolve(T param) {
        return resolver.resolve(param);
    }

    @Override
    public Binding bind(T param) {
        return bind(resolve(param));
    }

    @Override
    public Binding bind(Resolution resolution) {
        Objects.requireNonNull(resolution, "resolution");
        if (!(resolution instanceof DefaultResolution<?> own) || !own.createdBy(registry)) {
            throw new IllegalArgumentException("the resolution was not created by this context");
        }
        Deque<DefaultBinding> stack = bindings.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            bindings.set(stack);
        }
        DefaultBinding binding = new DefaultBinding(resolution, stack);
        stack.push(binding);
        return binding;
    }

    @Override
    public Resolution current() {
        Deque<DefaultBinding> stack = bindings.get();
        if (stack == null || stack.isEmpty()) {
            throw new ResolutionException(ResolutionException.Reason.NO_BINDING, String.format(
                    "no resolution is bound to thread [%s], bind one first: try (Binding b = context.bind(param)) { ... }",
                    threadLabel(Thread.currentThread())));
        }
        return stack.peek().resolution();
    }

    @Override
    public void clear() {
        bindings.remove();
    }

    @Override
    public <E> E proxy(Class<E> point) {
        requireInterface(point);
        return ExtensionProxies.proxy(this, point);
    }

    @Override
    public <E> List<E> proxyAll(Class<E> point) {
        requireInterface(point);
        return ExtensionProxies.proxyAll(this, point);
    }

    @Override
    public ExtensionCatalog catalog() {
        return registry.catalog();
    }

    private static String threadLabel(Thread thread) {
        // virtual threads have an empty name
        return thread.getName().isEmpty() ? "virtual-" + thread.threadId() : thread.getName();
    }

    private void requireInterface(Class<?> point) {
        if (point == null || !registry.points().containsKey(point)) {
            throw new IllegalArgumentException("extension point [" + (point == null ? null : point.getName()) + "] is not registered");
        }
    }

    private final class DefaultBinding implements Binding {
        private final Resolution resolution;
        private final Deque<DefaultBinding> stack;
        private final Thread owner = Thread.currentThread();
        private boolean closed;

        DefaultBinding(Resolution resolution, Deque<DefaultBinding> stack) {
            this.resolution = resolution;
            this.stack = stack;
        }

        @Override
        public Resolution resolution() {
            return resolution;
        }

        @Override
        public void close() {
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException(String.format(
                        "a Binding must be closed by the thread that opened it [%s], not by [%s]; to continue on another thread, bind the Resolution there",
                        threadLabel(owner), threadLabel(Thread.currentThread())));
            }
            if (closed) {
                return;
            }
            closed = true;
            if (!stack.contains(this)) {
                return;
            }
            // closing restores the previous binding; bindings opened after this one are dropped with it
            while (!stack.isEmpty()) {
                if (stack.pop() == this) {
                    break;
                }
            }
            if (stack.isEmpty() && bindings.get() == stack) {
                bindings.remove();
            }
        }
    }
}
