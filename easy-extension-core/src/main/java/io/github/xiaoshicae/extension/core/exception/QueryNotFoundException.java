package io.github.xiaoshicae.extension.core.exception;

public class QueryNotFoundException extends QueryException {
    public QueryNotFoundException(String message) {
        super(message);
    }

    public QueryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
