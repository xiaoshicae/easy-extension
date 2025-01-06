package io.github.xiaoshicae.extension.proxy.business;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import io.github.xiaoshicae.extension.proxy.exception.ProxyParamException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BusinessProxyFactoryTest {

    @Test
    public void testNewUsedAbility() throws Exception {
        Method newUsedAbilityMethod = BusinessProxyFactory.class.getDeclaredMethod("newUsedAbility", String.class, String.class, int.class);
        newUsedAbilityMethod.setAccessible(true); //

        BusinessProxyFactory<Object> factory = new BusinessProxyFactory<>();

        InvocationTargetException e;
        ProxyParamException te;

        e = assertThrows(InvocationTargetException.class, () -> newUsedAbilityMethod.invoke(factory, "biz", "", 0));
        te = (ProxyParamException) e.getTargetException();
        assertEquals("business [biz] use ability code can not be empty", te.getMessage());

        e = assertThrows(InvocationTargetException.class, () -> newUsedAbilityMethod.invoke(factory, "biz", "  ", 0));
        te = (ProxyParamException) e.getTargetException();
        assertEquals("business [biz] use ability code can not be empty", te.getMessage());

        e = assertThrows(InvocationTargetException.class, () -> newUsedAbilityMethod.invoke(factory, "biz", "a::b::c", 0));
        te = (ProxyParamException) e.getTargetException();
        assertEquals("business [biz] use ability code [a::b::c] priority delimiter[::] count can not more than 1", te.getMessage());

        e = assertThrows(InvocationTargetException.class, () -> newUsedAbilityMethod.invoke(factory, "biz", "::b", 0));
        te = (ProxyParamException) e.getTargetException();
        assertEquals("business [biz] use ability code [::b], code can not be empty", te.getMessage());

        e = assertThrows(InvocationTargetException.class, () -> newUsedAbilityMethod.invoke(factory, "biz", "a::b", 0));
        te = (ProxyParamException) e.getTargetException();
        assertEquals("business [biz] use ability [a::b] with priority must be int", te.getMessage());

        UsedAbility usedAbility = (UsedAbility) newUsedAbilityMethod.invoke(factory, "biz", "a", 1);
        assertEquals("a", usedAbility.code());
        assertEquals(1, usedAbility.priority());

        usedAbility = (UsedAbility) newUsedAbilityMethod.invoke(factory, "biz", "aa::3", 1);
        assertEquals("aa", usedAbility.code());
        assertEquals(3, usedAbility.priority());

        usedAbility = (UsedAbility) newUsedAbilityMethod.invoke(factory, "biz", " bb:3 :: 4", 1);
        assertEquals("bb:3", usedAbility.code());
        assertEquals(4, usedAbility.priority());

        usedAbility = (UsedAbility) newUsedAbilityMethod.invoke(factory, "biz", " bb:3 ::", 1);
        assertEquals("bb:3", usedAbility.code());
        assertEquals(1, usedAbility.priority());
    }

    @Test
    public void testNewBusinessProxy() throws ProxyException {
        ProxyException e;
        BusinessProxyFactory<Object> factory = new BusinessProxyFactory<>();

        e = assertThrows(ProxyException.class, () -> factory.newBusinessProxy(null));
        assertEquals("class should not be null", e.getMessage());

        e = assertThrows(ProxyException.class, () -> factory.newBusinessProxy(C1.class));
        assertEquals(String.format("business [%s] must implement [%s]", C1.class.getSimpleName(), Matcher.class.getName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> factory.newBusinessProxy(C3.class));
        assertEquals(String.format("code of @Business annotate on [%s] should not be blank", C3.class.getSimpleName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> factory.newBusinessProxy(C33.class));
        assertEquals(String.format("abilities of @Business annotate on [%s] can not contain blank ability code", C33.class.getSimpleName()), e.getMessage());

        IBusiness<Object> business = factory.newBusinessProxy(C4.class);
        assertEquals("c4", business.code());

        assertEquals(10, business.priority());
        assertEquals(6, business.usedAbilities().size());
        assertEquals("a", business.usedAbilities().get(0).code());
        assertEquals(1, business.usedAbilities().get(0).priority());
        assertEquals("b", business.usedAbilities().get(1).code());
        assertEquals(2, business.usedAbilities().get(1).priority());
        assertEquals("c", business.usedAbilities().get(2).code());
        assertEquals(10, business.usedAbilities().get(2).priority());
        assertEquals("d", business.usedAbilities().get(3).code());
        assertEquals(11, business.usedAbilities().get(3).priority());
        assertEquals("e", business.usedAbilities().get(4).code());
        assertEquals(20, business.usedAbilities().get(4).priority());
        assertEquals("f", business.usedAbilities().get(5).code());
        assertEquals(21, business.usedAbilities().get(5).priority());


        assertArrayEquals(new Class[]{E2.class, E3.class}, business.implementExtensionPoints().toArray());
        assertTrue(business.match("123"));
        assertFalse(business.match("456"));

        String s = ((C4) business).doE3();
        assertEquals("C4 doE3", s);
    }
}

class C1 {}

class C2 implements Matcher<Object> {
    @Override
    public Boolean match(Object param) {
        return null;
    }
}

@Business(code="")
class C3 implements Matcher<Object> {
    @Override
    public Boolean match(Object param) {
        return null;
    }
}


@Business(code="c33", abilities = {"", "a"})
class C33 implements Matcher<Object> {
    @Override
    public Boolean match(Object param) {
        return null;
    }
}

@Business(code="c4", priority = 10, abilities = {"a", "b", "c::10", "d", "e::20", "f"})
class C4 implements Matcher<Object>, E1, E2, E3 {
    @Override
    public Boolean match(Object param) {
        return Objects.equals(param, "123");
    }

    @Override
    public String doE3() {
        return "C4 doE3";
    }
}

interface E1{}

@ExtensionPoint
interface E2{}


@ExtensionPoint
interface E3{
    String doE3();
}
