package io.github.xiaoshicae.extension.proxy.ability;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;



public class AbilityProxyFactoryTest {

    @Test
    public void testNewAbilityProxy() throws ProxyException {
        ProxyException e;
        AbilityProxyFactory<Object> factory = new AbilityProxyFactory<>();

        e = assertThrows(ProxyException.class, () -> factory.newAbilityProxy(null));
        assertEquals("class should not be null", e.getMessage());

        e = assertThrows(ProxyException.class, () -> factory.newAbilityProxy(C1.class));
        assertEquals(String.format("ability [%s] must implement [%s]", C1.class.getSimpleName(), Matcher.class.getName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> factory.newAbilityProxy(C3.class));
        assertEquals(String.format("code of @Ability annotate on [%s] should not be blank", C3.class.getSimpleName()), e.getMessage());

        IAbility<Object> ability = factory.newAbilityProxy(C4.class);
        assertEquals("c4", ability.code());
        assertArrayEquals(new Class[]{E2.class, E3.class}, ability.implementExtensionPoints().toArray());
        assertTrue(ability.match("123"));
        assertFalse(ability.match("456"));

        String s = ((C4) ability).doE3();
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

@Ability(code="")
class C3 implements Matcher<Object> {
    @Override
    public Boolean match(Object param) {
        return null;
    }
}

@Ability(code="c4")
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
