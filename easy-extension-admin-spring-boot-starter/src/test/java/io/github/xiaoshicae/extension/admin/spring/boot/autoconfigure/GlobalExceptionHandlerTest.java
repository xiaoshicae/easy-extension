package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalExceptionHandlerTest {

    @Test
    public void testErrorHandlingIsScopedToTheAdminApi() {
        RestControllerAdvice advice = GlobalExceptionHandler.class.getAnnotation(RestControllerAdvice.class);

        assertNotNull(advice);
        assertArrayEquals(new Class<?>[]{EasyExtensionAdminAPI.class}, advice.assignableTypes(),
                "an unscoped advice would rewrite the errors of the host application's own controllers");
        assertEquals(0, advice.basePackages().length);
    }
}
