package io.github.xiaoshicae.extension.core.exception;

public class SessionParamException extends SessionException{
    public SessionParamException(String message) {
        super(message);
    }

    public SessionParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
