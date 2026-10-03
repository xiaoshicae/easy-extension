package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnnProxyConvertUtilsTest {
    @Test
    public void testResolveUsedAbility() throws Exception {
        ProxyException te;

        te = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.resolveUsedAbility("biz", "", 0));
        assertEquals("business [biz] used ability invalid, code should not be empty", te.getMessage());

        te = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.resolveUsedAbility("biz", "  ", 0));
        assertEquals("business [biz] used ability invalid, code should not be empty", te.getMessage());

        te = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.resolveUsedAbility("biz", "a::b::c", 0));
        assertEquals("business [biz] used ability [a::b::c] invalid, format error", te.getMessage());

        te = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.resolveUsedAbility("biz", "::b", 0));
        assertEquals("business [biz] used ability [::b] invalid, code should not be empty", te.getMessage());

        te = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.resolveUsedAbility("biz", "a::b", 0));
        assertEquals("business [biz] used ability [a::b] invalid, priority should be int", te.getMessage());

        UsedAbility usedAbility = AnnProxyConvertUtils.resolveUsedAbility("biz", "a", 1);
        assertEquals("a", usedAbility.code());
        assertEquals(1, usedAbility.priority());

        usedAbility = AnnProxyConvertUtils.resolveUsedAbility("biz", "aa::3", 1);
        assertEquals("aa", usedAbility.code());
        assertEquals(3, usedAbility.priority());

        usedAbility = AnnProxyConvertUtils.resolveUsedAbility("biz", " bb:3 :: 4", 1);
        assertEquals("bb:3", usedAbility.code());
        assertEquals(4, usedAbility.priority());

        usedAbility = AnnProxyConvertUtils.resolveUsedAbility("biz", " bb:3 ::", 1);
        assertEquals("bb:3", usedAbility.code());
        assertEquals(1, usedAbility.priority());
    }

    @Test
    public void testConvertAnnBusinessToProxy() throws ProxyException {
        ProxyException e;

        e = assertThrows(ProxyException.class, () -> AnnProxyConvertUtils.convertAnnBusinessToProxy(new C2()));
        assertEquals(String.format("business [%s] must annotated with @Business", C2.class.getSimpleName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> AnnProxyConvertUtils.convertAnnBusinessToProxy(new C3()));
        assertEquals(String.format("code of @Business annotate on [%s] should not be blank", C3.class.getSimpleName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> AnnProxyConvertUtils.convertAnnBusinessToProxy(new C33()));
        assertEquals(String.format("abilities of @Business annotate on [%s] can not contain blank ability code", C33.class.getSimpleName()), e.getMessage());

        IBusiness<Object> business = AnnProxyConvertUtils.convertAnnBusinessToProxy(new C4());
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


        assertEquals(List.of(E1.class, E2.class, E3.class), business.implementExtensionPoints());
        assertTrue(business.match("123"));
        assertFalse(business.match("456"));

        String s = ((E3) business).doE3();
        assertEquals("C4 doE3", s);
    }

    @Test
    public void testConvertAnnAbilityToProxy() throws ProxyException {
        ProxyException e;

        e = assertThrows(ProxyException.class, () -> AnnProxyConvertUtils.convertAnnAbilityToProxy(new Ab2()));
        assertEquals(String.format("ability [%s] must annotated with @Ability", Ab2.class.getSimpleName()), e.getMessage());

        e = assertThrows(ProxyException.class, () -> AnnProxyConvertUtils.convertAnnAbilityToProxy(new Ab3()));
        assertEquals(String.format("code of @Ability annotate on [%s] should not be blank", Ab3.class.getSimpleName()), e.getMessage());

        IAbility<Object> ability = AnnProxyConvertUtils.convertAnnAbilityToProxy(new Ab4());
        assertEquals("ab4", ability.code());

        assertEquals(List.of(E1.class, E2.class, E3.class), ability.implementExtensionPoints());
        assertTrue(ability.match("123"));
        assertFalse(ability.match("456"));

        String s = ((E3) ability).doE3();
        assertEquals("Ab4 doE3", s);
    }

    /**
     * What a container such as Spring AOP hands out: a JDK proxy that implements the interfaces of the class but
     * carries neither its annotations nor its class identity.
     */
    @SuppressWarnings("unchecked")
    private static Matcher<Object> containerProxyOf(Object target) {
        return (Matcher<Object>) Proxy.newProxyInstance(
                AnnProxyConvertUtilsTest.class.getClassLoader(),
                new Class<?>[]{Matcher.class, E1.class, E2.class, E3.class},
                (proxy, method, args) -> method.invoke(target, args));
    }

    @Test
    public void testTargetClassDefaultsToTheClassOfTheInstance() throws ProxyException {
        IAbility<Object> ability = AnnProxyConvertUtils.convertAnnAbilityToProxy(new Ab4());
        assertSame(Ab4.class, ((IProxy<?>) ability).getTargetClass());

        IBusiness<Object> business = AnnProxyConvertUtils.convertAnnBusinessToProxy(new C4());
        assertSame(C4.class, ((IProxy<?>) business).getTargetClass());
    }

    @Test
    public void testConvertAnnAbilityToProxyReadsMetadataFromTheTargetClass() throws ProxyException {
        Matcher<Object> containerProxy = containerProxyOf(new Ab4());

        ProxyException e = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.convertAnnAbilityToProxy(containerProxy));
        assertTrue(e.getMessage().startsWith("ability ["), e.getMessage());
        assertTrue(e.getMessage().endsWith("] must annotated with @Ability"), e.getMessage());

        IAbility<Object> ability = AnnProxyConvertUtils.convertAnnAbilityToProxy(containerProxy, Ab4.class);
        assertEquals("ab4", ability.code());
        assertEquals(List.of(E1.class, E2.class, E3.class), ability.implementExtensionPoints());
        assertSame(containerProxy, ((IProxy<?>) ability).getInstance(), "the container's proxy is what gets called");
        assertSame(Ab4.class, ((IProxy<?>) ability).getTargetClass());
        assertTrue(ability.match("123"));
        assertEquals("Ab4 doE3", ((E3) ability).doE3());
    }

    @Test
    public void testConvertAnnBusinessToProxyReadsMetadataFromTheTargetClass() throws ProxyException {
        Matcher<Object> containerProxy = containerProxyOf(new C4());

        ProxyException e = assertThrows(ProxyParamException.class, () -> AnnProxyConvertUtils.convertAnnBusinessToProxy(containerProxy));
        assertTrue(e.getMessage().startsWith("business ["), e.getMessage());
        assertTrue(e.getMessage().endsWith("] must annotated with @Business"), e.getMessage());

        IBusiness<Object> business = AnnProxyConvertUtils.convertAnnBusinessToProxy(containerProxy, C4.class);
        assertEquals("c4", business.code());
        assertEquals(10, business.priority());
        assertEquals(6, business.usedAbilities().size());
        assertEquals(List.of(E1.class, E2.class, E3.class), business.implementExtensionPoints());
        assertSame(containerProxy, ((IProxy<?>) business).getInstance(), "the container's proxy is what gets called");
        assertSame(C4.class, ((IProxy<?>) business).getTargetClass());
        assertTrue(business.match("123"));
        assertEquals("C4 doE3", ((E3) business).doE3());
    }

    @Test
    public void testConvertAnnExtensionPointGroupDefaultImplementationReadsMetadataFromTheTargetClass() throws ProxyException {
        Matcher<Object> containerProxy = containerProxyOf(new Ab4());

        IExtensionPointGroupDefaultImplementation<Object> defaultImpl =
                AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(containerProxy, Ab4.class);
        assertEquals(List.of(E1.class, E2.class, E3.class), defaultImpl.implementExtensionPoints());
        assertSame(containerProxy, ((IProxy<?>) defaultImpl).getInstance());
        assertSame(Ab4.class, ((IProxy<?>) defaultImpl).getTargetClass());
        assertEquals("Ab4 doE3", ((E3) defaultImpl).doE3());
    }

    @Test
    public void testTargetClassMustNotBeNull() {
        ProxyException e = assertThrows(ProxyParamException.class,
                () -> new io.github.xiaoshicae.extension.core.proxy.AbilityProxyFactory<>("ab4", new Ab4(), null, List.of(E1.class)));
        assertEquals("target class should not be null", e.getMessage());
    }
}

class Ab2 implements Matcher<MP> {
    @Override
    public boolean match(MP param) {
        return false;
    }
}


@Ability(code = "")
class Ab3 implements Matcher<MP> {
    @Override
    public boolean match(MP param) {
        return false;
    }
}


@Ability(code = "ab4")
class Ab4 implements Matcher<Object>, E1, E2, E3 {
    @Override
    public boolean match(Object param) {
        return Objects.equals(param, "123");
    }

    @Override
    public String doE3() {
        return "Ab4 doE3";
    }
}
