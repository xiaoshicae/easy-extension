package io.github.xiaoshicae.extension.core.interceptor;

/**
 * Around-advice for calls to extension implementations: tracing, metrics, timeouts, circuit breaking, audit logging.
 * <p>
 * An interceptor sees every method call that reaches a business, an ability or a default implementation through
 * the extension context ({@code getFirstMatchedExtension}, {@code getAllMatchedExtension}, {@code invoke...}) or
 * through an {@code @ExtensionInject} field. It can run code before and after, change the outcome, or not proceed at
 * all.
 * </p>
 * <pre>{@code
 * context.registerInterceptor(invocation -> {
 *     long start = System.nanoTime();
 *     try {
 *         return invocation.proceed();
 *     } finally {
 *         metrics.record(invocation.extensionPoint(), invocation.implementationCode(), System.nanoTime() - start);
 *     }
 * });
 * }</pre>
 * <p>
 * Interceptors are applied in registration order, the first registered being the outermost. An exception thrown
 * by the implementation reaches {@link ExtensionInvocation#proceed()} unwrapped, and what the interceptor throws
 * reaches the caller unwrapped as well; checked exceptions the extension point method does not declare are
 * wrapped by the JDK proxy in {@link java.lang.reflect.UndeclaredThrowableException}, as for any proxy.
 * </p>
 *
 * @since 3.4
 */
@FunctionalInterface
public interface ExtensionInterceptor {

    /**
     * Intercept one call.
     *
     * @param invocation the call; {@link ExtensionInvocation#proceed()} continues with the next interceptor or,
     *                   after the last one, with the implementation
     * @return the result to hand to the caller
     * @throws Throwable whatever should reach the caller
     */
    Object intercept(ExtensionInvocation invocation) throws Throwable;
}
