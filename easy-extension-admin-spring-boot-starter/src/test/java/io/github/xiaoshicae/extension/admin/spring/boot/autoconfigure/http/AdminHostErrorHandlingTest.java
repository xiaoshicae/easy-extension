package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http.AdminTestApp.Reply;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Having the admin starter on the classpath must not change how the host application answers its own errors: the
 * admin's error handler is for the admin API only.
 */
public class AdminHostErrorHandlingTest {
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
    public void testFaviconOfTheHostApplicationIsStillServed() throws Exception {
        // src/test/resources/static/favicon.ico of this module stands for the favicon of the host application
        assertEquals("HOST-FAVICON", AdminTestApp.get(context, "/favicon.ico").body());
    }

    @Test
    public void testHostEndpointStillWorks() throws Exception {
        Reply reply = AdminTestApp.get(context, "/host/ok");

        assertEquals(200, reply.status());
        assertEquals("ok", reply.body());
    }

    @Test
    public void testResponseStatusExceptionOfAHostControllerKeepsItsStatus() throws Exception {
        Reply reply = AdminTestApp.get(context, "/host/forbidden");

        assertEquals(403, reply.status());
        assertFalse(reply.body().contains("Internal server error"), reply.body());
    }

    @Test
    public void testUnknownPathOfTheHostApplicationIsNotFound() throws Exception {
        assertEquals(404, AdminTestApp.get(context, "/host/does-not-exist").status());
    }

    @Test
    public void testWrongMethodOnAHostEndpointIsMethodNotAllowed() throws Exception {
        assertEquals(405, AdminTestApp.send(context, "POST", "/host/ok").status());
    }
}
