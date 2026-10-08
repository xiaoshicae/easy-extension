package io.github.xiaoshicae.extension.spring.boot.autoconfigure.web;

import io.github.xiaoshicae.extension.core.Binding;
import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.Resolution;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * What "a {@link MatcherParamResolver} bean turns on automatic binding" does, spelled out. For every request that reaches
 * Spring MVC:
 * <ol>
 *   <li>{@code preHandle}: the resolver derives the matcher param from the request, the context resolves it (picks the
 *   business, evaluates the abilities) and binds the result to the handling thread;</li>
 *   <li>the controller, {@code @ControllerAdvice}s and view rendering run with that binding, so injected extension
 *   points and {@code context.first(..)} see this request's business;</li>
 *   <li>{@code afterCompletion}, or {@code afterConcurrentHandlingStarted} when the handler returned an async result:
 *   the binding is closed, because it belongs to the thread that opened it. The async dispatch goes through
 *   {@code preHandle} again, on its own thread.</li>
 * </ol>
 * Code outside Spring MVC (servlet filters, other threads, message listeners) is not covered: bind there with
 * {@link ExtensionContext#runWith(Object, Runnable)}. To register this interceptor yourself instead of declaring a
 * {@link MatcherParamResolver} bean:
 * <pre>{@code
 * registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> OrderParam.from(request)))
 *         .addPathPatterns("/api/**");
 * }</pre>
 *
 * @param <T> matcher param type
 */
public class ExtensionSessionInterceptor<T> implements AsyncHandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ExtensionSessionInterceptor.class);
    private static final String BINDINGS_ATTRIBUTE = ExtensionSessionInterceptor.class.getName() + ".BINDINGS";

    private final ExtensionContext<T> context;
    private final MatcherParamResolver<T> resolver;

    public ExtensionSessionInterceptor(ExtensionContext<T> context, MatcherParamResolver<T> resolver) {
        this.context = context;
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isErrorDispatch(request)) {
            return true;
        }
        Binding binding = context.bind(resolver.resolve(request));
        bindings(request).push(binding);
        if (logger.isDebugEnabled()) {
            logger.debug("[Easy Extension] bound {} {} to thread [{}], business [{}]; unbound when the request completes",
                    request.getMethod(), request.getRequestURI(), Thread.currentThread().getName(), businessOf(binding));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!isErrorDispatch(request)) {
            closeLatest(request);
        }
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isErrorDispatch(request)) {
            closeLatest(request);
        }
    }

    // logging must never break a request: tolerate custom bindings without a resolution or a trace
    private static String businessOf(Binding binding) {
        Resolution resolution = binding.resolution();
        ResolveTrace trace = resolution == null ? null : resolution.trace();
        return trace == null ? null : trace.matchedBusinessCode();
    }

    // the container's error page is rendered after the request failed (possibly because no business matched): never bind it
    private static boolean isErrorDispatch(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ERROR;
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
