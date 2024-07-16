package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.AbilityException;

import java.util.List;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


public class DefaultAbilityManagerTest {
    DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

    @Test
    public void testDefaultAbilityManager() throws AbilityException {
        AbilityException e;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        e = assertThrows(AbilityException.class, () -> manager.registerAbility(null));
        assertEquals("ability can not be null", e.getMessage());

        e = assertThrows(AbilityException.class, () -> manager.registerAbility(new Ability1()));
        assertEquals("ability Ability1 should implement at least one interface that annotated with @ExtensionPoint", e.getMessage());

        manager.registerAbility(new Ability2());
        e = assertThrows(AbilityException.class, () -> manager.registerAbility(new Ability2()));
        assertEquals("ability Ability2 already registered", e.getMessage());

        e = assertThrows(AbilityException.class, () -> manager.getAbility(null));
        assertEquals("abilityCode can not be null", e.getMessage());

        e = assertThrows(AbilityException.class, () -> manager.getAbility("x"));
        assertEquals("ability x not found", e.getMessage());

        IAbility<Object> ability2 = manager.getAbility("Ability2");
        assertNotNull(ability2);

        List<IAbility<Object>> abilities = manager.listAllAbilities();
        assertEquals(1, abilities.size());
        assertEquals(ability2, abilities.get(0));
    }
}
