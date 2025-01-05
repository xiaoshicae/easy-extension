package io.github.xiaoshicae.extension.core.exception;

public class RegisterParamException extends RegisterException {
    public RegisterParamException(String message) {
        super(message);
    }

    public RegisterParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
