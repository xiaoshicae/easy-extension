package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.IExtensionSession;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *
 * // Hand the request's identity to another thread: capture the chain, bind it there
 * ResolvedChain chain = ctx.currentChain();
 * executor.submit(() -> ExtensionSessionScope.runWith(ctx, chain, () -> service.handleAsync()));
 * }</pre>
 *
 * <h2>Handing a session over</h2>
 * <p>
 * {@link #restore(IExtensionSession, ResolvedChain)}, {@link #restoreScoped(IExtensionSession, String, ResolvedChain)}
 * and {@link #runWith(IExtensionSession, ResolvedChain, Supplier)} bind a chain captured elsewhere. The task they
 * wrap does not always run on a thread of its own: a direct executor, {@code CallerRunsPolicy} on a saturated pool
 * and parallel streams run it on the thread that handed it over, which has a session of its own. So they put the
 * thread back as it was: the session the scope held before is bound again, and only when there was none is the scope
 * removed (the default scope: every scope of the thread, as {@link #run(IExtensionSession, Object, Supplier)} does).
 * </p>
 *
 * <h2>Nesting</h2>
 * <p>
 * {@link #open(IExtensionSession, Object)}, {@link #openScoped(IExtensionSession, String, Object)} and
 * {@code run} put the thread back as it was as well, so they nest: service A, wrapped by an aspect that opens a scope,
 * calls service B, wrapped by the same aspect, and A's session is still there when B returns. Only when the scope held
 * no session before is it removed. A scope whose session cannot be initialized (no business matched) leaves the thread
 * without a session for that scope, the one it held before included: code that asked for another identity is not
 * served as the old one.
 * </p>
 */
public final class ExtensionSessionScope implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionSessionScope.class);

    private final IExtensionSession<?> session;

    /**
     * The scope this helper opened, or {@code null} for the default scope, whose cleanup removes everything.
     */
    private final String scope;

    /**
     * What the scope held before this helper bound a chain over it, to be bound again on close;
     * {@code null} when it held nothing, or when that cannot be told.
     */
    private final ResolvedChain previous;

    private ExtensionSessionScope(IExtensionSession<?> session, String scope, ResolvedChain previous) {
        this.session = session;
        this.scope = scope;
        this.previous = previous;
    }

    /**
     * Initialize a session bound to the current thread and return an
     * {@link AutoCloseable} whose {@link #close()} puts the thread back as it was: the session the default scope held
     * before is bound again (the helper nests); if there was none, {@link IExtensionSession#removeSession()} runs.
     */
    public static <T> ExtensionSessionScope open(IExtensionSession<T> session, T param) throws SessionException {
        ResolvedChain previous = chainOf(session, null);
        session.initSession(param);
        return new ExtensionSessionScope(session, null, previous);
    }

    /**
     * Scoped variant of {@link #open(IExtensionSession, Object)}. Closing it binds the session the scope held before
     * again, or, if there was none, removes the session of that scope; sessions of other scopes (the default scope
     * included) stay.
     */
    public static <T> ExtensionSessionScope openScoped(IExtensionSession<T> session, String scope, T param) throws SessionException {
        ResolvedChain previous = chainOf(session, scope);
        session.initSession(scope, param);
        return new ExtensionSessionScope(session, scope, previous);
    }

    /**
     * Bind a captured chain to the current thread (default scope) instead of initializing a session from a param,
     * and return an {@link AutoCloseable} whose {@link #close()} puts the thread back as it was: the session the
     * default scope held before is bound again; if there was none, {@link IExtensionSession#removeSession()} runs.
     *
     * @see IExtensionSession#bind(ResolvedChain)
     * @since 3.4
     */
    public static ExtensionSessionScope restore(IExtensionSession<?> session, ResolvedChain chain) throws SessionException {
        ResolvedChain previous = chainOf(session, null);
        session.bind(chain);
        return new ExtensionSessionScope(session, null, previous);
    }

    /**
     * Scoped variant of {@link #restore(IExtensionSession, ResolvedChain)}. Closing it binds the session the scope
     * held before again, or, if there was none, removes the session of that scope; other scopes stay.
     *
     * @since 3.4
     */
    public static ExtensionSessionScope restoreScoped(IExtensionSession<?> session, String scope, ResolvedChain chain) throws SessionException {
        ResolvedChain previous = chainOf(session, scope);
        session.bind(scope, chain);
        return new ExtensionSessionScope(session, scope, previous);
    }

    /**
     * Run {@code body} inside a freshly-initialized session and return its result.
     * Cleanup happens in a {@code finally} and puts the thread back as it was (see {@link #open(IExtensionSession, Object)}),
     * so a {@code run} inside a {@code run} leaves the outer session bound; exceptions from {@code body} propagate.
     */
    public static <T, R> R run(IExtensionSession<T> session, T param, Supplier<R> body) throws SessionException {
        try (ExtensionSessionScope ignored = open(session, param)) {
            return body.get();
        }
    }

    /**
     * Void-returning variant of {@link #run(IExtensionSession, Object, Supplier)}.
     */
    public static <T> void run(IExtensionSession<T> session, T param, Runnable body) throws SessionException {
        try (ExtensionSessionScope ignored = open(session, param)) {
            body.run();
        }
    }

    /**
     * Run {@code body} with a captured chain bound to the current thread, and return its result. The chain is
     * taken as it is, no matcher is evaluated. Cleanup happens in a {@code finally} and puts the thread back as it
     * was, so it is safe when the body runs on the thread that already has a session: that session is bound again
     * afterwards. When the thread had none, {@link IExtensionSession#removeSession()} runs. Exceptions from
     * {@code body} propagate.
     *
     * @see IExtensionSession#bind(ResolvedChain)
     * @since 3.4
     */
    public static <R> R runWith(IExtensionSession<?> session, ResolvedChain chain, Supplier<R> body) throws SessionException {
        try (ExtensionSessionScope ignored = restore(session, chain)) {
            return body.get();
        }
    }

    /**
     * Void-returning variant of {@link #runWith(IExtensionSession, ResolvedChain, Supplier)}.
     *
     * @since 3.4
     */
    public static void runWith(IExtensionSession<?> session, ResolvedChain chain, Runnable body) throws SessionException {
        try (ExtensionSessionScope ignored = restore(session, chain)) {
            body.run();
        }
    }

    @Override
    public void close() {
        if (previous != null && bindPrevious()) {
            return;
        }
        if (scope == null) {
            session.removeSession();
        } else {
            session.removeSession(scope);
        }
    }

    private boolean bindPrevious() {
        try {
            if (scope == null) {
                session.bind(previous);
            } else {
                session.bind(scope, previous);
            }
            return true;
        } catch (SessionException | UnsupportedOperationException e) {
            logger.warn("[Easy Extension] could not bind the session that was there before, it is removed instead: {}", e.getMessage());
            return false;
        }
    }

    /**
     * The chain the scope holds on this thread, {@code null} if it holds none or the session does not keep chains.
     */
    private static ResolvedChain chainOf(IExtensionSession<?> session, String scope) {
        try {
            return scope == null ? session.currentChain() : session.currentChain(scope);
        } catch (UnsupportedOperationException e) {
            // a session that does not keep chains cannot hand one back: there is nothing to put back
            return null;
        }
    }
}
