package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.proxy.defaultimpl.ExtensionPointDefaultImplProxyFactory;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


public class ExtensionPointDefaultImplProxyFactoryTest {
    @Test
    public void testNewExtensionPointDefaultImplProxy() throws ProxyException {
        ExtensionPointDefaultImplProxyFactory<Object> factory = new ExtensionPointDefaultImplProxyFactory<>();

        IExtensionPointGroupDefaultImplementation<Object> proxy = factory.newExtensionPointDefaultImplProxy(DefaultExtensionImpl.class);
        assertTrue(proxy.match(null));
        assertTrue(proxy.match("123"));
        assertTrue(proxy.match(123));
        assertEquals(Integer.MAX_VALUE, proxy.priority());
        assertEquals("system.extension.point.default.implementation", proxy.code());
        assertArrayEquals(new Class[]{EA.class, EB.class}, proxy.implementExtensionPoints().toArray());

        EA ea = (EA) proxy;
        assertEquals("da", ea.a());

        EB eb = (EB) proxy;
        assertEquals("db", eb.b());
    }
}

@ExtensionPoint
interface EA {
    String a();
}

@ExtensionPoint
interface EB {
    String b();
}

@ExtensionPointDefaultImplementation
class DefaultExtensionImpl implements EA, EB {

    @Override
    public String a() {
        return "da";
    }

    @Override
    public String b() {
        return "db";
    }
}
