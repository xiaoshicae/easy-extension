package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultExtensionPointGroupImplementationManagerTest {

    @Test
    public void testRegisterExtensionPointImplementationInstance() throws Exception {
        RegisterException e;
        DefaultExtensionPointGroupImplementationManager<Object> manager = new DefaultExtensionPointGroupImplementationManager<>();

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(null));
        assertEquals("instance should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceCodeNone()));
        assertEquals("instance code should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceNotImplIFace()));
        assertEquals("instance not implement extension point class [" + IFace.class.getName() + "]", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceImplClassNotInterface()));
        assertEquals("instance implement extension point class [" + String.class.getName() + "] invalid, class should be an interface type", e.getMessage());

        manager.registerExtensionPointImplementationInstance(new InstanceX());
        e = assertThrows(RegisterException.class, () -> manager.registerExtensionPointImplementationInstance(new InstanceX()));
        assertEquals(String.format("extension point [%s] with name [%s] already registered", IFace.class.getName(), "InstanceX"), e.getMessage());
    }

    @Test
    public void testGetExtensionPointImplementationInstance() throws Exception {
        QueryException e;

        DefaultExtensionPointGroupImplementationManager<Object> manager = new DefaultExtensionPointGroupImplementationManager<>();

        e = assertThrows(QueryException.class, () -> manager.getExtensionPointImplementationInstance(IFace.class, "InstanceX"));
        assertEquals("instance not found by extension point class [InstanceX] + name [IFace]", e.getMessage());

        InstanceX instanceX = new InstanceX();
        manager.registerExtensionPointImplementationInstance(instanceX);
        IFace instance = manager.getExtensionPointImplementationInstance(IFace.class, "InstanceX");
        assertSame(instanceX, instance);
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


class InstanceCodeNone extends BaseInstance {
    @Override
    public String code() {
        return null;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(String.class);
    }
}


class InstanceImplClassNotInterface extends BaseInstance {
    @Override
    public String code() {
        return "InstanceImplClassNotInterface";
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(String.class);
    }
}


interface IFace {
}

class InstanceNotImplIFace extends BaseInstance {
    @Override
    public String code() {
        return "InstanceNotImplIFace";
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IFace.class);
    }
}

class InstanceX extends BaseInstance implements IFace {
    @Override
    public String code() {
        return "InstanceX";
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IFace.class);
    }
}
