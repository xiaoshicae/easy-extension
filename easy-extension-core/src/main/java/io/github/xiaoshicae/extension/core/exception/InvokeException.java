package io.github.xiaoshicae.extension.core.exception;

/**
 * Runtime exception thrown during extension point invocation.
 * <p>
 * Extends {@link RuntimeException} instead of {@link ExtensionException} because
 * invoke operations are on the hot path and should not require explicit catch blocks.
 * </p>
 */
public class InvokeException extends RuntimeException {
    public InvokeException(String message) {
        super(message);
    }

    public InvokeException(String message, Throwable cause) {
        super(message, cause);
    }
}
