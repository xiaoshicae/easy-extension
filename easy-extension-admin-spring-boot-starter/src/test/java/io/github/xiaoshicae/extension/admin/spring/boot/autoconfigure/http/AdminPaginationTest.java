package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http.AdminTestApp.API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bad paging parameters are the caller's mistake: they are answered with a 4xx (or served), never with a 500 and an
 * ERROR log with a stack trace.
 */
public class AdminPaginationTest {
    private static ConfigurableApplicationContext context;

    @BeforeAll
    public static void startApplication() {
        context = AdminTestApp.start();
    }

    @AfterAll
    public static void stopApplication() {
        context.close();
    }

    @Test
    public void testNegativeOffsetIsNotAServerError() throws Exception {
        int status = AdminTestApp.get(context, API + "/extension-points?offset=-1").status();

        assertTrue(status < 500, "status " + status);
    }

    @Test
    public void testNonNumericLimitIsABadRequest() throws Exception {
        assertEquals(400, AdminTestApp.get(context, API + "/extension-points?limit=abc").status());
    }
}
