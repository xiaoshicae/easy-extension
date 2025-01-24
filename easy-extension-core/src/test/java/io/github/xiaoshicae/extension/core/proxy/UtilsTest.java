package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import org.junit.jupiter.api.Test;

import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UtilsTest {
    @Test
    public void test() throws ProxyParamException {
        Exception e;
        e = assertThrows(ProxyParamException.class, () -> Utils.validateInstance(new Object(), List.of(Object.class)));
        assertEquals("The extension point should be an interface: java.lang.Object", e.getMessage());

        e = assertThrows(ProxyParamException.class, () -> Utils.validateInstance(new Object(), List.of(Ext1.class)));
        assertEquals("The instance does not implement the extension point: " + Ext1.class.getName(), e.getMessage());

        e = assertThrows(ProxyParamException.class, () -> Utils.validateInstance(new InnerClass(), List.of(InnerInterface.class)));
        assertEquals("Modifier of extension point [%s] should be public".formatted(InnerInterface.class.getName()), e.getMessage());
    }
}


interface InnerInterface {
}

class InnerClass implements InnerInterface {
}