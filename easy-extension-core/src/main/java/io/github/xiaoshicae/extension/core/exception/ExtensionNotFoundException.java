package io.github.xiaoshicae.extension.core.exception;

public class ExtensionNotFoundException extends ExtensionException {
    public ExtensionNotFoundException(String message) {
        super(message);
    }

    public ExtensionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}