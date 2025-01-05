package io.github.xiaoshicae.extension.core.exception;

public class QueryParamException extends QueryException {
    public QueryParamException(String message) {
        super(message);
    }

    public QueryParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
