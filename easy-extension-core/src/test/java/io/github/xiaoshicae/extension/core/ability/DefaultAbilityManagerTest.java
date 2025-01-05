package io.github.xiaoshicae.extension.core.ability;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DefaultAbilityManagerTest {

    @Test
    public void testRegisterAbility() throws RegisterException {
        RegisterException e;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        e = assertThrows(RegisterException.class, () -> manager.registerAbility(null));
        assertEquals("ability should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerAbility(new Ability1()));
        assertEquals("ability [Ability1] should implement at least one extension point", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerAbility(new Ability2()));
        assertEquals("ability [Ability2] implement extension point class [java.lang.String] invalid, class should be an interface type", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerAbility(new Ability3()));
        assertEquals("ability [Ability3] not implement extension point class [" + Ext2.class.getName() + "]", e.getMessage());

        manager.registerAbility(new Ability4());
        e = assertThrows(RegisterException.class, () -> manager.registerAbility(new Ability4()));
        assertEquals("ability [Ability4] already registered", e.getMessage());
    }

    @Test
    public void testGetAbility() throws QueryException {
        QueryException e;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        e = assertThrows(QueryException.class, () -> manager.getAbility(null));
        assertEquals("abilityCode should not be null", e.getMessage());

        e = assertThrows(QueryException.class, () -> manager.getAbility("c"));
        assertEquals("ability not found by code [c]", e.getMessage());

        try {
            manager.registerAbility(new Ability4());
        } catch (RegisterException e1) {
           throw new RuntimeException(e1);
        }
        IAbility<Object> ability = manager.getAbility("Ability4");
        assertEquals("Ability4", ability.code());
    }

    @Test
    public void testListAllAbilities() throws QueryException {
        QueryException e;
        DefaultAbilityManager<Object> manager = new DefaultAbilityManager<>();

        try {
            manager.registerAbility(new Ability4());
            manager.registerAbility(new Ability5());
        } catch (RegisterException e1) {
            throw new RuntimeException(e1);
        }

        List<IAbility<Object>> iAbilities = manager.listAllAbilities();
        assertEquals(2, iAbilities.size());
        assertEquals("Ability4", iAbilities.get(0).code());
        assertEquals("Ability5", iAbilities.get(1).code());

        manager.getAbility("Ability5");

        iAbilities = manager.listAllAbilities();
        assertEquals(2, iAbilities.size());
        assertEquals("Ability4", iAbilities.get(0).code());
        assertEquals("Ability5", iAbilities.get(1).code());
    }
}
