package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Makes the framework's dynamic proxies transparent for exceptions.
 * <p>
 * Every proxy layer forwards a call with {@link Method#invoke(Object, Object...)}, which reports whatever the
 * target threw as an {@link InvocationTargetException}. A JDK proxy cannot rethrow that (no interface method
 * declares it) and wraps it in {@link java.lang.reflect.UndeclaredThrowableException}, so with several layers a plain
 * {@code IllegalStateException} used to reach the caller as four nested wrappers, and a declared checked exception
 * could not be caught as such. The methods here unwrap the {@link InvocationTargetException} and rethrow the
 * original exception.
 * </p>
 * <p>
 * Internal helper of the framework's proxy classes, not meant to be called by application code.
 * </p>
 *
 * @since 3.4
 */
public final class InvocationExceptions {

    /**
     * JVM system property that restores the wrapping of earlier releases
     * ({@code -Deasy-extension.legacy-exception-wrapping=true}). Read when a proxy is created.
     * It is a JVM system property only: setting it in {@code application.yml} (where the starter's own
     * {@code easy-extension.*} properties live) has no effect.
     * Provided for one release to ease migration; it will be removed in 4.0.
     */
    public static final String LEGACY_WRAPPING_PROPERTY = "easy-extension.legacy-exception-wrapping";

    private static final Logger logger = LoggerFactory.getLogger(InvocationExceptions.class);
    private static final AtomicBoolean LEGACY_ANNOUNCED = new AtomicBoolean();

    private InvocationExceptions() {
    }

    /**
     * Whether the pre-3.4 exception wrapping was requested through {@link #LEGACY_WRAPPING_PROPERTY}.
     * The first time it is, that is logged (once), so that the mode is visible in the logs.
     */
    public static boolean legacyWrapping() {
        boolean legacy = Boolean.getBoolean(LEGACY_WRAPPING_PROPERTY);
        if (legacy && LEGACY_ANNOUNCED.compareAndSet(false, true)) {
            logger.warn("[Easy Extension] exceptions thrown by extension implementations are wrapped as in releases before 3.4, "
                    + "because the JVM system property {} is set. It is meant for migrating and will be removed in 4.0.",
                    LEGACY_WRAPPING_PROPERTY);
        }
        return legacy;
    }

    /**
     * Invoke {@code method} on {@code target}, rethrowing what the target threw instead of an
     * {@link InvocationTargetException}.
     *
     * @param legacy keep the {@link InvocationTargetException} (behavior of releases before 3.4)
     */
    public static Object invoke(Method method, Object target, Object[] args, boolean legacy) throws Throwable {
        try {
            if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                // registering an extension point only asks for an interface; one that is not public can be called
                // reflectively only with the access check off
                method.trySetAccessible();
            }
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (legacy || cause == null) {
                throw e;
            }
            throw cause;
        }
    }

    /**
     * What to throw from a proxy when no extension could be resolved for the call.
     * <p>
     * The {@link QueryException} is checked and the extension point methods usually do not declare it, which would
     * surface as {@link java.lang.reflect.UndeclaredThrowableException}. It is thrown as is when {@code method}
     * declares it (or a supertype of it), otherwise as an {@link InvokeException} carrying it as the cause.
     * </p>
     *
     * @param target description of what was invoked, e.g. {@code PriceExtension}
     * @param legacy throw the {@link QueryException} as is (behavior of releases before 3.4)
     */
    public static Throwable queryFailure(Method method, String target, QueryException failure, boolean legacy) {
        if (legacy) {
            return failure;
        }
        for (Class<?> declared : method.getExceptionTypes()) {
            if (declared.isInstance(failure)) {
                return failure;
            }
        }
        return new InvokeException(String.format("invoke %s#%s failed, %s", target, method.getName(), failure.getMessage()), failure);
    }
}
