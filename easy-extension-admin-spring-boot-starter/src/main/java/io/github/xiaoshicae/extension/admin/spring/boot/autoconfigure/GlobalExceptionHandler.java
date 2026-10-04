package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Error handling of the admin API. It applies to the admin API only: a {@code @RestControllerAdvice} without a
 * selector applies to every controller of the application, and a handler for {@code Exception} would turn the errors
 * of the host application (403, 404, 405, validation failures ...) into a 500.
 */
@RestControllerAdvice(assignableTypes = EasyExtensionAdminAPI.class)
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleIllegalArgument(IllegalArgumentException e) {
        logger.warn("Easy Extension Admin API received invalid request: {}", e.getMessage());
        return Response.paramError(e.getMessage());
    }

    /**
     * A parameter that cannot be converted ({@code limit=abc}) is the caller's mistake.
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleTypeMismatch(TypeMismatchException e) {
        logger.warn("Easy Extension Admin API received invalid request: {}", e.getMessage());
        return Response.paramError(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleException(Exception e) {
        logger.error("Easy Extension Admin API encountered an error", e);
        return Response.fail("Internal server error");
    }
}
