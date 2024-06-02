package io.github.xiaoshicae.extension.core.test;

import io.github.xiaoshicae.extension.core.exception.AbilityException;
import org.junit.Test;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.ability.DefaultAbilityManager;
import io.github.xiaoshicae.extension.core.ability.IAbility;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultAbilityManagerTest {
    @Test
    public void testRegisterAbility() throws AbilityException {
        AbilityException exception;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        exception = assertThrows(AbilityException.class, () -> manager.registerAbility(null));
        assertEquals("ability can not be null", exception.getMessage());

        exception = assertThrows(AbilityException.class, () -> manager.registerAbility(new Ability1()));
        assertEquals("ability Ability1 should implement at least one interface that annotated with @ExtensionPoint", exception.getMessage());

        manager.registerAbility(new Ability2());
        exception = assertThrows(AbilityException.class, () -> manager.registerAbility(new Ability2()));
        assertEquals("ability Ability2 already registered", exception.getMessage());
    }

    @Test
    public void testGetAbility() throws AbilityException {
        AbilityException exception;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        exception = assertThrows(AbilityException.class, () -> manager.getAbility(null));
        assertEquals("abilityCode can not be null", exception.getMessage());

        exception = assertThrows(AbilityException.class, () -> manager.getAbility("biz"));
        assertEquals("ability biz not found", exception.getMessage());

        manager.registerAbility(new Ability2());
        IAbility<Object> a = manager.getAbility("Ability2");
        assertNotNull(a);
        assertEquals("Ability2", a.code());
        assertEquals(1, a.implementsExtensions().size());
        assertEquals(ExtA.class, a.implementsExtensions().get(0));
        List<IAbility<Object>> abilityList = manager.listAllAbilities();
        assertEquals(1, abilityList.size());
        assertEquals(a, abilityList.get(0));
    }
}


class Ability1 extends AbstractAbility<Object> {
    @Override
    public String code() {
        return "Ability1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability1");
    }
}

class Ability2 extends AbstractAbility<Object> implements ExtA {
    @Override
    public String code() {
        return "Ability2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability2");
    }

    @Override
    public String extA() {
        return "Ability2 ExtA";
    }
}
