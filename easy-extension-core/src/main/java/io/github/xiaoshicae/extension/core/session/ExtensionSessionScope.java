package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.IExtensionSession;
import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.function.Supplier;

/**
 * Try-with-resources / lambda helper that guarantees
 * {@link IExtensionSession#removeSession()} runs after the body — even in
 * non-Servlet environments (WebFlux, MQ consumers, scheduled tasks, batch jobs)
 * where the {@code SessionCleanupFilter} does not apply.
 * <p>
 * This closes the single biggest ThreadLocal-leak foot-gun: any thread that
 * calls {@code initSession(...)} must also call {@code removeSession()}, and
 * forgetting to do so in a pooled-thread environment pins session data to
 * worker threads indefinitely. Prefer this helper for all non-Servlet code.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Lambda style — recommended
 * var result = ExtensionSessionScope.run(ctx, param, () -> service.handle());
 *
 * // Or with the AutoCloseable form for imperative blocks
 * try (var scope = ExtensionSessionScope.open(ctx, param)) {
 *     service.handle();
 * }
 * }</pre>
 */
public final class ExtensionSessionScope implements AutoCloseable {

    private final IExtensionSession<?> session;

    private ExtensionSessionScope(IExtensionSession<?> session) {
        this.session = session;
    }

    /**
     * Initialize a session bound to the current thread and return an
     * {@link AutoCloseable} whose {@link #close()} always calls
     * {@link IExtensionSession#removeSession()}.
     */
    public static <T> ExtensionSessionScope open(IExtensionSession<T> session, T param) throws SessionException {
        session.initSession(param);
        return new ExtensionSessionScope(session);
    }

    /**
     * Scoped variant of {@link #open(IExtensionSession, Object)}.
     */
    public static <T> ExtensionSessionScope openScoped(IExtensionSession<T> session, String scope, T param) throws SessionException {
        session.initScopedSession(scope, param);
        return new ExtensionSessionScope(session);
    }

    /**
     * Run {@code body} inside a freshly-initialized session and return its result.
     * Cleanup happens in a {@code finally}; exceptions from {@code body} propagate.
     */
    public static <T, R> R run(IExtensionSession<T> session, T param, Supplier<R> body) throws SessionException {
        session.initSession(param);
        try {
            return body.get();
        } finally {
            session.removeSession();
        }
    }

    /**
     * Void-returning variant of {@link #run(IExtensionSession, Object, Supplier)}.
     */
    public static <T> void run(IExtensionSession<T> session, T param, Runnable body) throws SessionException {
        session.initSession(param);
        try {
            body.run();
        } finally {
            session.removeSession();
        }
    }

    @Override
    public void close() {
        session.removeSession();
    }
}
