package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Ready-made {@link AdminAuthenticationProvider} that validates HTTP Basic
 * credentials against a configured username/password pair.
 * <p>
 * Intended as a "turn it on and go" option for small deployments. For anything
 * more sophisticated (session-based login, SSO, Spring Security chains, JWT
 * validation, etc.), implement {@link AdminAuthenticationProvider} directly or
 * stack another provider alongside this one — the filter requires all providers
 * to succeed.
 * </p>
 * <p>
 * Passwords are compared in constant time to avoid trivial timing side-channels.
 * Credentials are never logged.
 * </p>
 */
public class BasicAuthAdminAuthenticationProvider implements AdminAuthenticationProvider {

    private static final String SCHEME = "Basic ";

    private final String expectedUsername;
    private final byte[] expectedPasswordBytes;
    private final String realm;

    public BasicAuthAdminAuthenticationProvider(String username, String password, String realm) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        this.expectedUsername = username;
        this.expectedPasswordBytes = password.getBytes(StandardCharsets.UTF_8);
        this.realm = realm == null || realm.isBlank() ? "Easy Extension Admin" : realm;
    }

    @Override
    public boolean authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(SCHEME)) {
            return false;
        }
        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(header.substring(SCHEME.length()).trim()),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException bad) {
            return false;
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            return false;
        }
        String user = decoded.substring(0, colon);
        String pass = decoded.substring(colon + 1);
        return expectedUsername.equals(user) && constantTimeEquals(expectedPasswordBytes,
                pass.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getChallenge() {
        return "Basic realm=\"" + realm.replace("\"", "\\\"") + "\"";
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
