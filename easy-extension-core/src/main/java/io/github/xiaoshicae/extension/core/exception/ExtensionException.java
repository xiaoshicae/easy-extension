package io.github.xiaoshicae.extension.core.exception;

public class ExtensionException extends Exception {
    public ExtensionException(String message) {
        super(message);
    }

    public ExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
