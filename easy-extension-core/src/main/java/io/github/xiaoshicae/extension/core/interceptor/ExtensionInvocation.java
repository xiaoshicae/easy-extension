package io.github.xiaoshicae.extension.core.interceptor;

import java.lang.reflect.Method;

/**
 * One call to an extension implementation, as seen by an {@link ExtensionInterceptor}.
 *
 * @since 4.0
 */
public interface ExtensionInvocation {

    /**
     * The extension point interface that was called.
     */
    Class<?> extensionPoint();

    /**
     * Code of the business, ability or default implementation that serves the call.
     */
    String implementationCode();

    /**
     * The implementation that serves the call. It may be a framework proxy; ask
     * {@code IProxy#getTargetClass()} for the class behind it.
     */
    Object implementation();

    /**
     * The extension point method that was called.
     */
    Method method();

    /**
     * The arguments of the call; empty for a method without parameters, never {@code null}.
     */
    Object[] arguments();

    /**
     * Continue with the next interceptor or, after the last one, call the implementation.
     * An exception of the implementation is rethrown as it is.
     *
     * @return what the implementation returned
     * @throws Throwable what the implementation threw
     */
    Object proceed() throws Throwable;
}
