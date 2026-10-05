package io.github.xiaoshicae.extension.core;

/**
 * A {@link Resolution} bound to the current thread, which is what injected extension proxies and
 * {@link ExtensionContext#first(Class)} read. Bindings nest: closing one restores the previous binding.
 * A binding belongs to the thread that opened it and must be closed by that thread; to continue the request on
 * another thread, bind the {@link Resolution} there ({@link ExtensionContext#bind(Resolution)}).
 * <pre>{@code
 * try (Binding binding = context.bind(param)) {
 *     service.handle();
 * }
 * }</pre>
 */
public interface Binding extends AutoCloseable {

    Resolution resolution();

    /**
     * Unbinds, restoring the binding that was current before this one. Idempotent.
     *
     * @throws IllegalStateException if called from a thread other than the one that opened the binding
     */
    @Override
    void close();
}
