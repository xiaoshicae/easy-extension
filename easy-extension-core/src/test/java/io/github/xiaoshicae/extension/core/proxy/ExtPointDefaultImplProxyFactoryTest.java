package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtPointDefaultImplProxyFactoryTest {

    @Test
    public void test() throws ProxyException {
        Exception e = assertThrows(ProxyParamException.class, () -> new ExtPointDefaultImplProxyFactory<>(new DefaultImplInstance(), List.of(Ext1.class, Ext2.class, Ext3.class)));
        assertEquals("The instance does not implement the extension point: io.github.xiaoshicae.extension.core.proxy.Ext3", e.getMessage());

        ExtPointDefaultImplProxyFactory<MP> factory = new ExtPointDefaultImplProxyFactory<>(new DefaultImplInstance(), List.of(Ext1.class, Ext2.class));

        IExtensionPointGroupDefaultImplementation<MP> dynamicProxy = factory.getProxy();
        assertEquals("system.extension.point.default.implementation", dynamicProxy.code());
        assertTrue(dynamicProxy.match(new MP("X")));
        assertTrue(dynamicProxy.match(new MP("XX")));
        assertEquals(List.of(Ext1.class, Ext2.class), dynamicProxy.implementExtensionPoints());

        IProxy<DefaultImplInstance> p = (IProxy<DefaultImplInstance>) dynamicProxy;
        assertEquals(DefaultImplInstance.class, p.getInstance().getClass());

        assertEquals("DefaultImplInstance doSomething1", ((Ext1) dynamicProxy).doSomething1());
        assertEquals("DefaultImplInstance doSomething2", ((Ext2) dynamicProxy).doSomething2());
    }
}


class DefaultImplInstance implements Matcher<MP>, Ext1, Ext2 {
    @Override
    public String doSomething1() {
        return "DefaultImplInstance doSomething1";
    }

    @Override
    public String doSomething2() {
        return "DefaultImplInstance doSomething2";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "X");
    }
}
