package io.github.xiaoshicae.extension.proxy.exception;

public class ProxyException extends Exception {
    public ProxyException(String message) {
        super(message);
    }

    public ProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
