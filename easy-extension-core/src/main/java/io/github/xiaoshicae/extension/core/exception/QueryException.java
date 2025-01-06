package io.github.xiaoshicae.extension.core.exception;

public class QueryException extends ExtensionException {
    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
