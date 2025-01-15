package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbilityProxyFactoryTest {

    @Test
    public void test() throws ProxyException {
        Exception e = assertThrows(ProxyParamException.class, () -> new AbilityProxyFactory<>(
                "x.x.x",
                new AbilityXInstance(),
                List.of(Ext1.class, Ext2.class, Ext3.class)
        ));
        assertEquals("The instance does not implement the extension point: io.github.xiaoshicae.extension.core.proxy.Ext3", e.getMessage());

        e = assertThrows(ProxyParamException.class, () -> new AbilityProxyFactory<>(
                "x.x.x",
                new AbilityYInstance(),
                List.of(AbilityExtInner.class)
        ));
        assertEquals("Modifier of extension point [io.github.xiaoshicae.extension.core.proxy.AbilityExtInner] should be public", e.getMessage());


        AbilityProxyFactory<MP> factory = new AbilityProxyFactory<>(
                "x.x.x",
                new AbilityXInstance(),
                List.of(Ext1.class, Ext2.class)
        );

        IAbility<MP> dynamicProxy = factory.getProxy();
        assertEquals("x.x.x", dynamicProxy.code());
        assertTrue(dynamicProxy.match(new MP("X")));
        assertFalse(dynamicProxy.match(new MP("XX")));
        assertEquals(List.of(Ext1.class, Ext2.class), dynamicProxy.implementExtensionPoints());

        IProxy<AbilityXInstance> p = (IProxy<AbilityXInstance>) dynamicProxy;
        assertEquals(AbilityXInstance.class, p.getInstance().getClass());

        assertEquals("AbilityXInstance doSomething1", ((Ext1) dynamicProxy).doSomething1());
        assertEquals("AbilityXInstance doSomething2", ((Ext2) dynamicProxy).doSomething2());
    }
}


class AbilityXInstance implements Matcher<MP>, Ext1, Ext2 {
    @Override
    public String doSomething1() {
        return "AbilityXInstance doSomething1";
    }

    @Override
    public String doSomething2() {
        return "AbilityXInstance doSomething2";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "X");
    }
}


class AbilityYInstance implements Matcher<MP>, AbilityExtInner {
    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "Y");
    }
}

interface AbilityExtInner {
}