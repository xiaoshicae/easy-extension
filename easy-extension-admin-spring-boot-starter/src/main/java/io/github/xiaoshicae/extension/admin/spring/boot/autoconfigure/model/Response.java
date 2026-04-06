package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import java.time.Instant;

public class Response<T> {
    private static final String SUCCESS = "SUCCESS";
    private static final String SUCCESS_CODE = "A00000";
    private static final String FAIL_CODE = "B00000";
    private static final String PARAM_ERROR_CODE = "B00001";

    private final T data;
    private final String code;
    private final String msg;
    private final String timestamp;
    private final Integer total;

    public Response(T data, String code, String msg) {
        this(data, code, msg, null);
    }

    public Response(T data, String code, String msg, Integer total) {
        this.data = data;
        this.code = code;
        this.msg = msg;
        this.timestamp = Instant.now().toString();
        this.total = total;
    }

    public static <T> Response<T> OK(T data) {
        return new Response<>(data, SUCCESS_CODE, SUCCESS);
    }

    public static <T> Response<T> OK(T data, int total) {
        return new Response<>(data, SUCCESS_CODE, SUCCESS, total);
    }

    public static Response<Void> fail(String msg) {
        return new Response<>(null, FAIL_CODE, msg);
    }

    public static Response<Void> paramError(String msg) {
        return new Response<>(null, PARAM_ERROR_CODE, msg);
    }

    /**
     * @deprecated Use {@link #fail(String)} instead. Kept for backward compatibility.
     */
    @Deprecated
    public static Response<Void> Fail(String msg) {
        return fail(msg);
    }

    public T getData() {
        return data;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getTotal() {
        return total;
    }
}
