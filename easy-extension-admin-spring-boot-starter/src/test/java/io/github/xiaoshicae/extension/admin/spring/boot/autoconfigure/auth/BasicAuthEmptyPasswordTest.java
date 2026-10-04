package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A password that is configured as {@code ${ADMIN_PASSWORD:}} and not set must not turn the built-in authentication
 * into one that anybody who knows the user name passes.
 */
public class BasicAuthEmptyPasswordTest {

    @Test
    public void testEmptyPasswordIsRejectedAtConstruction() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new BasicAuthAdminAuthenticationProvider("admin", "", "Admin"));

        assertEquals("password must not be empty", e.getMessage());
    }
}
