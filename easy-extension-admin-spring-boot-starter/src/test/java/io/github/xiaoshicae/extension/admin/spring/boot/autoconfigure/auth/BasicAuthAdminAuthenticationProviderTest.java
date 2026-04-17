package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BasicAuthAdminAuthenticationProviderTest {

    private static HttpServletRequest requestWithHeader(String authHeader) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Authorization")).thenReturn(authHeader);
        return req;
    }

    private static String basic(String user, String pass) {
        String creds = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void acceptsValidCredentials() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertTrue(p.authenticate(requestWithHeader(basic("admin", "s3cr3t"))));
    }

    @Test
    void rejectsWrongPassword() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertFalse(p.authenticate(requestWithHeader(basic("admin", "wrong"))));
    }

    @Test
    void rejectsWrongUsername() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertFalse(p.authenticate(requestWithHeader(basic("root", "s3cr3t"))));
    }

    @Test
    void rejectsMissingHeader() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertFalse(p.authenticate(requestWithHeader(null)));
    }

    @Test
    void rejectsNonBasicScheme() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertFalse(p.authenticate(requestWithHeader("Bearer abcd")));
    }

    @Test
    void rejectsMalformedBase64() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        assertFalse(p.authenticate(requestWithHeader("Basic !!!not-base64!!!")));
    }

    @Test
    void rejectsNoColon() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "Admin");
        String raw = Base64.getEncoder().encodeToString("noColonAtAll".getBytes(StandardCharsets.UTF_8));
        assertFalse(p.authenticate(requestWithHeader("Basic " + raw)));
    }

    @Test
    void passwordContainingColonIsSupported() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "a:b:c", "Admin");
        assertTrue(p.authenticate(requestWithHeader(basic("admin", "a:b:c"))));
    }

    @Test
    void challengeIncludesRealm() {
        var p = new BasicAuthAdminAuthenticationProvider("admin", "s3cr3t", "My Realm");
        assertEquals("Basic realm=\"My Realm\"", p.getChallenge());
    }

    @Test
    void rejectsEmptyUsernameAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new BasicAuthAdminAuthenticationProvider("", "pw", "r"));
        assertThrows(IllegalArgumentException.class,
                () -> new BasicAuthAdminAuthenticationProvider("admin", null, "r"));
    }
}
