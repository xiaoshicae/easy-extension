package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtensionPointManagerTest {

    @Test
    public void testRegisterExtensionPointImplementationInstance() throws ExtensionException {
        RegisterException e;
        DefaultExtensionPointManager manager = new DefaultExtensionPointManager();

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(null,null, null));
        assertEquals("extension point class should not be null", e.getMessage());

        class NotInterfaceClass {}
        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(NotInterfaceClass.class,null, null));
        assertEquals("extension point class should not be interface type", e.getMessage());

        interface ExtensionPoint1 {}

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(ExtensionPoint1.class,null, null));
        assertEquals("name should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(ExtensionPoint1.class, "name", null));
        assertEquals("instance should not be null", e.getMessage());

        interface ExtensionPoint2 {}

        class ExtensionPointImpl1 implements ExtensionPoint1 {}
        class ExtensionPointImpl2 implements ExtensionPoint1 {}
        class ExtensionPointImpl3 implements ExtensionPoint2 {}

        manager.registerExtensionPointImplementationInstance(ExtensionPoint1.class, "name", new ExtensionPointImpl1());
        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(ExtensionPoint1.class, "name", new ExtensionPointImpl2()));
        assertEquals("extension point [" + ExtensionPoint1.class.getName() + "] with name [name] already registered", e.getMessage());
        manager.registerExtensionPointImplementationInstance(ExtensionPoint1.class, "name2", new ExtensionPointImpl1());

        manager.registerExtensionPointImplementationInstance(ExtensionPoint2.class, "name", new ExtensionPointImpl3());
    }

    @Test
    public void testGetExtensionPointImplementationInstance() throws ExtensionException {
        QueryException exception;
        DefaultExtensionPointManager manager = new DefaultExtensionPointManager();

        exception = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(null, null));
        assertEquals("extension point class should not be null", exception.getMessage());

        class NotInterfaceClass {}
        exception = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(NotInterfaceClass.class, null));
        assertEquals("extension point class should be an interface type", exception.getMessage());

        interface ExtensionPoint {}
        exception = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(ExtensionPoint.class, null));
        assertEquals("name should not be null", exception.getMessage());

        exception = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(ExtensionPoint.class, "n"));
        assertEquals("instance with name [n] of extension point ["+ExtensionPoint.class.getSimpleName()+"] not found", exception.getMessage());

        class ExtensionPointImpl1 implements ExtensionPoint {}
        ExtensionPointImpl1 instance = new ExtensionPointImpl1();
        manager.registerExtensionPointImplementationInstance(ExtensionPoint.class, "name", instance);
        ExtensionPoint getInstance = manager.getExtensionPointImplementationInstance(ExtensionPoint.class, "name");
        assertEquals(instance, getInstance);
    }
}

