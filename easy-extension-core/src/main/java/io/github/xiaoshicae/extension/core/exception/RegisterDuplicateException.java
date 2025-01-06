package io.github.xiaoshicae.extension.core.exception;

public class RegisterDuplicateException extends RegisterException {
    public RegisterDuplicateException(String message) {
        super(message);
    }

    public RegisterDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}
