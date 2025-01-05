package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

public class Response<T> {
    private static String SUCCESS = "SUCCESS";
    private static String SUCCESS_CODE = "A00000";
    private static String FAIL_CODE = "B00000";

    private final T data;
    private final String code;
    private final String msg;



    public Response(T data, String code, String msg) {
        this.data = data;
        this.code = code;
        this.msg = msg;
    }

    public static <T> Response<T> OK(T data) {
        return new Response<>(data, SUCCESS_CODE, SUCCESS);
    }

    public static <Void> Response<Void> Fail(String msg) {
        return new Response<>(null, FAIL_CODE, msg);
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
}
