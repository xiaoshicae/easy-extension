package io.github.xiaoshicae.extension.core.exception;

/**
 * Root of all exceptions thrown by the framework. Unchecked.
 */
public class ExtensionException extends RuntimeException {
    public ExtensionException(String message) {
        super(message);
    }

    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
