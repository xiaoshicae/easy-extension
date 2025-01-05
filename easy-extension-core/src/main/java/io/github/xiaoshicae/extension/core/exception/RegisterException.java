package io.github.xiaoshicae.extension.core.exception;

public class RegisterException extends ExtensionException {
    public RegisterException(String message) {
        super(message);
    }

    public RegisterException(String message, Throwable cause) {
        super(message, cause);
    }
}
