package io.github.xiaoshicae.extension.core.exception;

public class InvokeException extends ExtensionException {
    public InvokeException(String message) {
        super(message);
    }

    public InvokeException(String message, Throwable cause) {
        super(message, cause);
    }
}
