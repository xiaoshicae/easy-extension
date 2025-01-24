package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessProxyFactoryTest {
    @Test
    public void testNewProxyFailed() {
        Exception e = assertThrows(ProxyParamException.class, () -> new BusinessProxyFactory<>(
                "x.x.x",
                10,
                List.of(new UsedAbility("a1", 1), new UsedAbility("a2", 2)),
                new BusinessAInstance(),
                List.of(Ext1.class, Ext2.class, Ext3.class)
        ));
        assertEquals("The instance does not implement the extension point: io.github.xiaoshicae.extension.core.proxy.Ext3", e.getMessage());
    }

    @Test
    public void testNewProxyWithoutAnnotation() throws ProxyException {
        List<UsedAbility> usedAbilities = List.of(new UsedAbility("a1", 1), new UsedAbility("a2", 2));
        BusinessProxyFactory<MP> factory = new BusinessProxyFactory<>(
                "x.x.x",
                10,
                usedAbilities,
                new BusinessAInstance(),
                List.of(Ext1.class, Ext2.class)
        );

        IBusiness<MP> dynamicProxy = factory.getProxy();
        assertEquals("x.x.x", dynamicProxy.code());
        assertEquals(10, dynamicProxy.priority());
        assertEquals(usedAbilities, dynamicProxy.usedAbilities());
        assertTrue(dynamicProxy.match(new MP("X")));
        assertFalse(dynamicProxy.match(new MP("XX")));
        assertEquals(List.of(Ext1.class, Ext2.class), dynamicProxy.implementExtensionPoints());

        IProxy<BusinessAInstance> p = (IProxy<BusinessAInstance>) dynamicProxy;
        assertEquals(BusinessAInstance.class, p.getInstance().getClass());

        assertEquals("BusinessAInstance doSomething1", ((Ext1) dynamicProxy).doSomething1());
        assertEquals("BusinessAInstance doSomething2", ((Ext2) dynamicProxy).doSomething2());
    }

    @Test
    public void testNewProxyWitAnnotation() throws ProxyException {
        IBusiness<MP> dynamicProxy2 = AnnProxyConvertUtils.convertAnnBusinessToProxy(new BusinessZZInstance());
        assertEquals("zzz", dynamicProxy2.code());
        assertEquals(0, dynamicProxy2.priority());
        assertEquals("a1", dynamicProxy2.usedAbilities().get(0).code());
        assertEquals(1, dynamicProxy2.usedAbilities().get(0).priority());
        assertEquals("a2", dynamicProxy2.usedAbilities().get(1).code());
        assertEquals(2, dynamicProxy2.usedAbilities().get(1).priority());
        assertTrue(dynamicProxy2.match(new MP("X")));
        assertFalse(dynamicProxy2.match(new MP("XX")));
        assertEquals(List.of(Ext1.class, Ext2.class), dynamicProxy2.implementExtensionPoints());

        IProxy<BusinessZZInstance> p2 = (IProxy<BusinessZZInstance>) dynamicProxy2;
        assertEquals(BusinessZZInstance.class, p2.getInstance().getClass());

        assertEquals("BusinessZZInstance doSomething1", ((Ext1) dynamicProxy2).doSomething1());
        assertEquals("BusinessZZInstance doSomething2", ((Ext2) dynamicProxy2).doSomething2());
    }
}


class BusinessAInstance implements Matcher<MP>, Ext1, Ext2 {
    @Override
    public String doSomething1() {
        return "BusinessAInstance doSomething1";
    }

    @Override
    public String doSomething2() {
        return "BusinessAInstance doSomething2";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "X");
    }
}


@Business(code = "zzz", abilities = {"a1::1", "a2::2"})
class BusinessZZInstance implements Matcher<MP>, Ext1, Ext2 {
    @Override
    public String doSomething1() {
        return "BusinessZZInstance doSomething1";
    }

    @Override
    public String doSomething2() {
        return "BusinessZZInstance doSomething2";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "X");
    }
}
