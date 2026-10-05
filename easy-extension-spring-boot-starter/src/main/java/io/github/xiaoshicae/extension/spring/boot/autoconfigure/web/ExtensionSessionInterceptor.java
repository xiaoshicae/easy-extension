package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Binds the resolution of the request to the handling thread: {@code preHandle} opens the binding, and it is closed when
 * handling completes or hands over to another thread (asynchronous request processing), because the binding belongs
 * to the thread that opened it. An asynchronous dispatch goes through {@code preHandle} again, on its own thread.
 *
 * @param <T> matcher param type
 */
public class ExtensionSessionInterceptor<T> implements AsyncHandlerInterceptor {
    private static final String BINDINGS_ATTRIBUTE = ExtensionSessionInterceptor.class.getName() + ".BINDINGS";

    private final ExtensionContext<T> context;
    private final MatcherParamResolver<T> resolver;

    public ExtensionSessionInterceptor(ExtensionContext<T> context, MatcherParamResolver<T> resolver) {
        this.context = context;
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Binding binding = context.bind(resolver.resolve(request));
        bindings(request).push(binding);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        closeLatest(request);
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        closeLatest(request);
    }

    @SuppressWarnings("unchecked")
    private static Deque<Binding> bindings(HttpServletRequest request) {
        Deque<Binding> bindings = (Deque<Binding>) request.getAttribute(BINDINGS_ATTRIBUTE);
        if (bindings == null) {
            bindings = new ArrayDeque<>();
            request.setAttribute(BINDINGS_ATTRIBUTE, bindings);
        }
        return bindings;
    }

    private static void closeLatest(HttpServletRequest request) {
        Deque<Binding> bindings = bindings(request);
        if (!bindings.isEmpty()) {
            bindings.pop().close();
        }
    }
}
