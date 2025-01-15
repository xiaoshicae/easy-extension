package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
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

public class BusinessProxyFactoryTest {

    @Test
    public void test() throws ProxyException {
        List<UsedAbility> usedAbilities = List.of(new UsedAbility("a1", 1), new UsedAbility("a2", 2));
        Exception e = assertThrows(ProxyParamException.class, () -> new BusinessProxyFactory<>(
                "x.x.x",
                10,
                usedAbilities,
                new BusinessAInstance(),
                List.of(Ext1.class, Ext2.class, Ext3.class)
        ));
        assertEquals("The instance does not implement the extension point: io.github.xiaoshicae.extension.core.proxy.Ext3", e.getMessage());

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
