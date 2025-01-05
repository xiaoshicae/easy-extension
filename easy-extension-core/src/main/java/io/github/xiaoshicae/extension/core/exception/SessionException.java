package io.github.xiaoshicae.extension.core.exception;

public class SessionException extends ExtensionException {
    public SessionException(String message) {
        super(message);
    }

    public SessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
