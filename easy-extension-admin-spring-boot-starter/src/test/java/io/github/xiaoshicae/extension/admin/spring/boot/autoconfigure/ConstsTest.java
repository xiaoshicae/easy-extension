package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.Consts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstsTest {

    @Test
    void shouldHaveCorrectDefaults() {
        assertEquals("/easy-extension-admin", Consts.ADMIN_HOME_PATH);
        assertEquals("/easy-extension-api", Consts.API_URL_SUFFIX);
        assertEquals("/easy-extension-admin-ui/latest/index.html", Consts.ADMIN_UI_RESOURCE_HOME_PATH);
        assertEquals("latest", Consts.ADMIN_UI_VERSION);
        assertEquals(500, Consts.DEFAULT_MAX_PAGINATION_LIMIT);
    }

    @Test
    void shouldNotUseSpelExpressions() {
        // Ensure paths are plain strings, not SpEL expressions
        assertFalse(Consts.ADMIN_HOME_PATH.contains("${"));
        assertFalse(Consts.ADMIN_HOME_PATH.contains("#{"));
        assertFalse(Consts.API_URL_SUFFIX.contains("${"));
        assertFalse(Consts.API_URL_SUFFIX.contains("#{"));
    }
}
