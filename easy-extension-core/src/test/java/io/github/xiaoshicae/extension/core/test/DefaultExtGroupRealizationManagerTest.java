package io.github.xiaoshicae.extension.core.test;

import org.junit.Test;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;
import io.github.xiaoshicae.extension.core.extension.AbstractExtGroupRealization;
import io.github.xiaoshicae.extension.core.extension.DefaultExtGroupRealizationManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DefaultExtGroupRealizationManagerTest {
    @Test
    public void testRegisterExtGroupRealization() throws ExtensionException {
        ExtensionException exception;
        DefaultExtGroupRealizationManager manager = new DefaultExtGroupRealizationManager();

        exception = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(null, null));
        assertEquals("name can not be null", exception.getMessage());

        exception = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(null, "extGroupRealization"));
        assertEquals("instance can not be null", exception.getMessage());

        manager.registerExtGroupRealization(new ExtGroupRealization2(), "ExtGroupRealization2");
        exception = assertThrows(ExtensionException.class, () -> manager.registerExtGroupRealization(new ExtGroupRealization2(), "ExtGroupRealization2"));
        assertEquals("instance io.github.xiaoshicae.extension.core.test.ExtA with name ExtGroupRealization2 already registered", exception.getMessage());
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
        String s = extA.extA();
        assertEquals("ExtGroupRealization2 extA", s);
    }
}


class ExtGroupRealization1 extends AbstractExtGroupRealization<Object> {
    @Override
    public String code() {
        return "ExtGroupRealization1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization1");
    }
}


class ExtGroupRealization2 extends AbstractExtGroupRealization<Object> implements ExtA {
    @Override
    public String code() {
        return "ExtGroupRealization2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization2");
    }

    @Override
    public String extA() {
        return "ExtGroupRealization2 extA";
    }
}
