package io.github.xiaoshicae.extension.core.exception;

/**
 * Root of all exceptions thrown by the framework. Unchecked.
 */
public class ExtensionException extends RuntimeException {
    /** Creates the exception. */
    public ExtensionException(String message) {
        super(message);
    }

    /** Creates the exception with a cause. */
    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
