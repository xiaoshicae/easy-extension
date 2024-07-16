package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtGroupRealizationManagerTest {
    DefaultExtGroupRealizationManager manager = new DefaultExtGroupRealizationManager();

    @Test
    public void testDefaultExtGroupRealizationManager() throws ExtensionException {
        ExtensionException e;

        e = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(null, null));
        assertEquals("name can not be null", e.getMessage());

        e = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(null, "extGroupRealization"));
        assertEquals("instance can not be null", e.getMessage());

        manager.registerExtGroupRealization(new ExtGroupRealization1(), "ExtGroupRealization1");

        manager.registerExtGroupRealization(new ExtGroupRealization2(), "ExtGroupRealization2");

        e = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(new ExtGroupRealization3(), "ExtGroupRealization2"));

        assertEquals("instance io.github.xiaoshicae.extension.core.extension.ExtA with name ExtGroupRealization2 already registered", e.getMessage());
    }

    @Test
    public void testGetExtGroupRealization() throws ExtensionException {
        ExtensionException exception;
        DefaultExtGroupRealizationManager manager = new DefaultExtGroupRealizationManager();

        exception = assertThrows(ExtensionException.class, () -> manager.getExtGroupRealization(null, null));
        assertEquals("name can not be null", exception.getMessage());

        exception = assertThrows(ExtensionException.class, () -> manager.getExtGroupRealization(null, ""));
        assertEquals("extensionType can not be null", exception.getMessage());

        exception = assertThrows(ExtensionException.class, () -> manager.getExtGroupRealization(ExtGroupRealization1.class, ""));
        assertEquals("extensionType must be an interface", exception.getMessage());

        exception = assertThrows(ExtensionException.class, () -> manager.getExtGroupRealization(IBusiness.class, ""));
        assertEquals("extensionType must be annotated with @ExtensionPoint", exception.getMessage());

        exception = assertThrows(ExtensionException.class, () -> manager.getExtGroupRealization(ExtA.class, "x"));
        assertEquals("extension x not found", exception.getMessage());

        manager.registerExtGroupRealization(new ExtGroupRealization2(), "ExtGroupRealization2");
        ExtA extA = manager.getExtGroupRealization(ExtA.class, "ExtGroupRealization2");
    }
}

