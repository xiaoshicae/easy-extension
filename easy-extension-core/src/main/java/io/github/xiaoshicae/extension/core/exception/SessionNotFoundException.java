package io.github.xiaoshicae.extension.core.exception;

public class SessionNotFoundException extends SessionException{
    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
