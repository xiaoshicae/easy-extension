package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http.AdminTestApp.Reply;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http.AdminTestApp.API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * With built-in Basic authentication configured, the admin API must ask for credentials whichever way the request
 * path is written or mounted: the filter and Spring MVC have to agree on what the path is.
 */
public class AdminAuthenticationPathTest {
    private static final String[] AUTH_PROPERTIES = {
            "easy-extension.admin.auth.basic.username=admin",
            "easy-extension.admin.auth.basic.password=secret"};
    private static final String CREDENTIALS = "Basic " + Base64.getEncoder().encodeToString("admin:secret".getBytes(StandardCharsets.UTF_8));

    private static ConfigurableApplicationContext context;

    @BeforeAll
    public static void startApplication() {
        context = AdminTestApp.start(AUTH_PROPERTIES);
    }

    @AfterAll
    public static void stopApplication() {
        context.close();
    }

    @Test
    public void testPlainRequestIsAuthenticated() throws Exception {
        assertEquals(401, AdminTestApp.get(context, API + "/config-info").status());
        assertEquals(200, AdminTestApp.get(context, API + "/config-info", "Authorization", CREDENTIALS).status());
    }

    @Test
    public void testPathParameterOnTheAdminSegmentDoesNotBypassAuthentication() throws Exception {
        // Spring MVC drops ";x=y" when it matches the path, the filter looked at the raw request URI
        Reply reply = AdminTestApp.get(context, "/easy-extension-admin;x=y/easy-extension-api/config-info");

        assertEquals(401, reply.status(), reply.body());
    }

    @Test
    public void testPercentEncodedPathDoesNotBypassAuthentication() throws Exception {
        // %65 is "e": Spring MVC decodes the path before it matches, the filter looked at the raw request URI
        Reply reply = AdminTestApp.get(context, "/%65asy-extension-admin/easy-extension-api/config-info");

        assertEquals(401, reply.status(), reply.body());
    }

    /** Ways of writing the path of the admin API that the container or the MVC path matching may read differently. */
    private static final String[] ODDLY_WRITTEN_API_PATHS = {
            "/foo/../easy-extension-admin/easy-extension-api/config-info",
            "/easy-extension-admin/./easy-extension-api/config-info",
            "/easy-extension-admin//easy-extension-api/config-info",
            "//easy-extension-admin/easy-extension-api/config-info",
            "/easy-extension-admin/easy-extension-api/../easy-extension-api/config-info",
            "/easy-extension-admin/easy-extension-api;x=y/config-info",
            "/easy-extension-admin/%65asy-extension-api/config-info"};

    private static void assertNoneIsServedWithoutCredentials(ConfigurableApplicationContext application) throws Exception {
        for (String path : ODDLY_WRITTEN_API_PATHS) {
            Reply reply = AdminTestApp.get(application, path);

            assertNotEquals(200, reply.status(), path + " was served without credentials: " + reply.body());
        }
    }

    @Test
    public void testPathsThatTheContainerNormalizesAreNotServedWithoutCredentials() throws Exception {
        // whatever the container or Spring MVC makes of these, the API must not answer 200 without credentials
        assertNoneIsServedWithoutCredentials(context);
    }

    @Test
    public void testAntPathMatcherModeDoesNotBypassAuthentication() throws Exception {
        // Spring Boot 3.x can still be told to match with the AntPathMatcher, which reads a path differently (it
        // collapses "//" and has its own idea of ";x=y"); a Spring Boot that dropped the property ignores it
        try (ConfigurableApplicationContext ant = AdminTestApp.start(concat(AUTH_PROPERTIES, "spring.mvc.pathmatch.matching-strategy=ant_path_matcher"))) {
            assertNoneIsServedWithoutCredentials(ant);
            assertEquals(401, AdminTestApp.get(ant, "/easy-extension-admin;x=y/easy-extension-api/config-info").status());
        }
    }

    @Test
    public void testContextPathDoesNotBypassAuthentication() throws Exception {
        try (ConfigurableApplicationContext withContextPath = AdminTestApp.start(concat(AUTH_PROPERTIES, "server.servlet.context-path=/app"))) {
            // getRequestURI() includes the context path
            Reply reply = AdminTestApp.get(withContextPath, "/app" + API + "/config-info");

            assertEquals(401, reply.status(), reply.body());
        }
    }

    @Test
    public void testAdminPathWithoutLeadingSlashIsStillProtected() throws Exception {
        try (ConfigurableApplicationContext withOtherPath = AdminTestApp.start(concat(AUTH_PROPERTIES, "easy-extension.admin.path=ops"))) {
            Reply reply = AdminTestApp.get(withOtherPath, "/ops/easy-extension-api/config-info");

            assertEquals(401, reply.status(), reply.body());
        }
    }

    private static String[] concat(String[] first, String... more) {
        String[] all = new String[first.length + more.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(more, 0, all, first.length, more.length);
        return all;
    }
}
