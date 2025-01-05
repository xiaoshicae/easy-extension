package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultExtensionPointGroupImplementationManagerTest {

    @Test
    public void testRegisterExtensionPointImplementationInstance() throws Exception {
        RegisterException e;
        DefaultExtensionPointGroupImplementationManager<Object> manager = new DefaultExtensionPointGroupImplementationManager<>();

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(null, null));
        assertEquals("instance should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceX(), null));
        assertEquals("name should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceX(), "n"));
        assertEquals("instance implement extension point class [" + String.class.getName() + "] invalid, class should be an interface type", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceY(), "n"));
        assertEquals("instance not implement extension point class [" + IFace.class.getName() + "]", e.getMessage());

        manager.registerExtensionPointImplementationInstance(new InstanceZ(), "n");
    }

    @Test
    public void testGetExtensionPointImplementationInstance() throws Exception {
        QueryException e;

        DefaultExtensionPointGroupImplementationManager<Object> manager = new DefaultExtensionPointGroupImplementationManager<>();

        e = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(IFace.class, "InstanceZ"));
        assertEquals("instance with name [InstanceZ] of extension point [" + IFace.class.getSimpleName() + "] not found", e.getMessage());

        manager.registerExtensionPointImplementationInstance(new InstanceZ(), "n");
        IFace instance = manager.getExtensionPointImplementationInstance(IFace.class, "n");
        assertNotNull(instance);
    }
}

class BaseInstance implements IExtensionPointGroupImplementation<Object> {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public String code() {
        return "";
    }

    @Override
    public Boolean match(Object param) {
        return null;
    }
}

class InstanceX extends BaseInstance {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(String.class);
    }
}

interface IFace {
}

class InstanceY extends BaseInstance {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IFace.class);
    }
}

class InstanceZ extends BaseInstance implements IFace {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IFace.class);
    }

    @Override
    public String code() {
        return "InstanceZ";
    }
}