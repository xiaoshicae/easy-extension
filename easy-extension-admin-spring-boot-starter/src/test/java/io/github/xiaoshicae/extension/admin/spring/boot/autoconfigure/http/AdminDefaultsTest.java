package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.http.AdminTestApp.API;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What leaving a documented option of the admin empty does.
 */
public class AdminDefaultsTest {

    @Test
    public void testEmptyBasicAuthUsernameLeavesTheBuiltInAuthenticationOff() throws Exception {
        // "leave empty to ship no built-in auth", typically ${ADMIN_USER:} with the variable not set
        try (ConfigurableApplicationContext context = AdminTestApp.start(
                "easy-extension.admin.auth.basic.username=", "easy-extension.admin.auth.basic.password=")) {
            assertEquals(200, AdminTestApp.get(context, API + "/config-info").status());
        }
    }
}
