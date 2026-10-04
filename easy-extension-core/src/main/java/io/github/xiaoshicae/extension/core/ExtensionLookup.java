package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupImplementationManager;
import io.github.xiaoshicae.extension.core.interceptor.ExtensionInterceptor;
import io.github.xiaoshicae.extension.core.session.IScopedSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The hot path: given the chain bound to the session, find the extension implementation to call.
 * <p>
 * Walks the chain's codes in priority order and asks the registry for an implementation of the extension point under
 * each. A miss is the normal case (a business implements only some of the extension points, the rest is served
 * further down the chain), so lookups answer {@code null} instead of throwing: no exception is created per miss.
 * What is found is handed out as it is, or wrapped for the registered interceptors if there are any.
 * </p>
 *
 * @param <T> matcher param class
 */
final class ExtensionLookup<T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultExtensionContext.class);
    private static final String LOG_PREFIX = DefaultExtensionContext.LOG_PREFIX;

    private final IScopedSessionManager session;
    private final IExtensionPointGroupImplementationManager<T> implementations;
    private final boolean enableLogger;

    /**
     * The interceptors (in registration order) together with the extension implementations wrapped for exactly those:
     * extension point -> code -> wrapper. A wrapper only depends on the interceptors, so it is built once and reused
     * by every call.
     * <p>
     * The two live in one immutable holder that is replaced as a whole when an interceptor is added. A call that
     * already read the old holder carries on with the old interceptors and the old cache, so it can never put a
     * wrapper built for stale interceptors into the cache that later calls use.
     * </p>
     *
     * @param interceptors the interceptors wrapped around extension implementations
     * @param wrappers     the wrappers built for {@code interceptors}
     */
    private record Interception(List<ExtensionInterceptor> interceptors, Map<Class<?>, Map<String, Object>> wrappers) {
    }

    private final Object interceptionLock = new Object();

    private volatile Interception interception = new Interception(List.of(), new ConcurrentHashMap<>());

    ExtensionLookup(IScopedSessionManager session, IExtensionPointGroupImplementationManager<T> implementations, boolean enableLogger) {
        this.session = session;
        this.implementations = implementations;
        this.enableLogger = enableLogger;
    }

    void addInterceptor(ExtensionInterceptor interceptor) {
        synchronized (interceptionLock) {
            List<ExtensionInterceptor> next = new ArrayList<>(interception.interceptors());
            next.add(interceptor);
            // what was wrapped for the previous interceptors is stale: start from an empty cache
            interception = new Interception(Collections.unmodifiableList(next), new ConcurrentHashMap<>());
        }
    }

    <E> E first(String scope, Class<E> extensionType) throws QueryException {
        if (scope == null) {
            throw new QueryNotFoundException("scope should not be null");
        }
        boolean defaultScope = DefaultExtensionContext.DEFAULT_SCOPE.equals(scope);
        List<String> matchedCodes = matchedCodes(scope);
        if (enableLogger) {
            if (defaultScope) {
                logger.info("{} get first matched Extension<{}>, all candidate codes: [{}]",
                        LOG_PREFIX, extensionType.getSimpleName(), String.join(" > ", matchedCodes));
            } else {
                logger.info("{} get first matched Extension<{}> with scope: [{}], all candidate codes: [{}]",
                        LOG_PREFIX, extensionType.getSimpleName(), scope, String.join(" > ", matchedCodes));
            }
        }
        try {
            return firstOf(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            if (defaultScope) throw e;
            throw inScope("first", extensionType, scope, e);
        }
    }

    <E> List<E> all(String scope, Class<E> extensionType) throws QueryException {
        if (scope == null) {
            throw new QueryNotFoundException("scope should not be null");
        }
        boolean defaultScope = DefaultExtensionContext.DEFAULT_SCOPE.equals(scope);
        List<String> matchedCodes = matchedCodes(scope);
        if (enableLogger) {
            if (defaultScope) {
                logger.info("{} get all matched Extension<{}>, all candidate codes: [{}]",
                        LOG_PREFIX, extensionType.getSimpleName(), String.join(" > ", matchedCodes));
            } else {
                logger.info("{} get all matched Extension<{}> with scope: [{}], all candidate codes: [{}]",
                        LOG_PREFIX, extensionType.getSimpleName(), scope, String.join(" > ", matchedCodes));
            }
        }
        try {
            return allOf(scope, extensionType, matchedCodes);
        } catch (QueryException e) {
            if (defaultScope) throw e;
            throw inScope("all", extensionType, scope, e);
        }
    }

    /**
     * What a lookup in a named scope throws: what the default scope throws (the same type, so that it can be caught
     * the same way, as {@code IExtensionFactory} documents) and the reason, with the scope in front of it.
     *
     * @param which {@code first} or {@code all}
     */
    private static QueryException inScope(String which, Class<?> extensionType, String scope, QueryException cause) {
        String message = String.format("get %s matched Extension<%s> with scope: [%s] failed, %s",
                which, extensionType.getSimpleName(), scope, cause.getMessage());
        return cause instanceof QueryNotFoundException ? new QueryNotFoundException(message, cause) : new QueryException(message, cause);
    }

    private <E> E firstOf(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        for (int i = 0; i < matchedCodes.size(); i++) {
            String code = matchedCodes.get(i);
            E extension = implementations.findExtensionPointImplementationInstance(extensionType, code);
            if (extension != null) {
                if (enableLogger) {
                    logger.info("{} get first matched Extension<{}>{}, hit instance with code: [{}]", LOG_PREFIX, extensionType.getSimpleName(), scopePrefix(scope), code);
                }
                return intercepted(extensionType, code, extension);
            }
            if (enableLogger) {
                logger.debug("{} get first matched Extension<{}>{}, instance with code: [{}] not matched, will be ignored", LOG_PREFIX, extensionType.getSimpleName(), scopePrefix(scope), code);
            }
        }
        throw new QueryNotFoundException(notFoundMessage(extensionType, matchedCodes));
    }

    private <E> List<E> allOf(String scope, Class<E> extensionType, List<String> matchedCodes) throws QueryException {
        List<E> extensions = new ArrayList<>();
        List<String> hitCodes = enableLogger ? new ArrayList<>() : null;
        for (int i = 0; i < matchedCodes.size(); i++) {
            String code = matchedCodes.get(i);
            E extension = implementations.findExtensionPointImplementationInstance(extensionType, code);
            if (extension != null) {
                extensions.add(intercepted(extensionType, code, extension));
                if (hitCodes != null) {
                    hitCodes.add(code);
                }
            } else if (enableLogger) {
                logger.debug("{} get all matched Extension<{}>{}s, instance with code: [{}] not matched, will be ignored", LOG_PREFIX, extensionType.getSimpleName(), scopePrefix(scope), code);
            }
        }

        if (enableLogger) {
            logger.info("{} get all matched Extension<{}>{}, hit instance with codes: [{}]", LOG_PREFIX, extensionType.getSimpleName(), scopePrefix(scope), String.join(" > ", hitCodes));
        }
        return extensions;
    }

    private static String scopePrefix(String scope) {
        return DefaultExtensionContext.DEFAULT_SCOPE.equals(scope) ? "" : " with scope: [%s]".formatted(scope);
    }

    private static String notFoundMessage(Class<?> extensionType, List<String> matchedCodes) {
        if (DefaultsRegistry.isMandatory(extensionType)) {
            return String.format("Extension<%s> not found, it is a mandatory extension point and none of [%s] implements it",
                    extensionType.getName(), String.join(" > ", matchedCodes));
        }
        return String.format("Extension<%s> not found", extensionType.getName());
    }

    /**
     * The implementation as handed to callers: wrapped for the registered interceptors, or as it is when there are none.
     */
    @SuppressWarnings("unchecked")
    private <E> E intercepted(Class<E> extensionType, String code, E implementation) {
        // read once: the interceptors and the cache that belongs to them are used together or not at all
        Interception current = interception;
        if (current.interceptors().isEmpty()) {
            return implementation;
        }
        // Read before computing: computeIfAbsent allocates the lambda on every call, and takes the lock of the bin
        // whenever the key is not the first node in it, which is the steady state here (the wrapper exists).
        Map<Class<?>, Map<String, Object>> wrappers = current.wrappers();
        Map<String, Object> byCode = wrappers.get(extensionType);
        if (byCode == null) {
            byCode = wrappers.computeIfAbsent(extensionType, k -> new ConcurrentHashMap<>());
        }
        Object wrapper = byCode.get(code);
        if (wrapper == null) {
            wrapper = byCode.computeIfAbsent(code, k -> InterceptingProxy.create(extensionType, code, implementation, current.interceptors()));
        }
        return (E) wrapper;
    }

    private List<String> matchedCodes(String scope) throws QueryException {
        try {
            return session.getScopedMatchedCodes(scope);
        } catch (SessionException e) {
            throw new QueryNotFoundException(e.getMessage());
        }
    }
}
