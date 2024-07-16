package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import java.util.List;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtContextTest {

    @Test
    public void testRegister() throws Exception {
        ExtensionException e;
        DefaultExtContext<Object> context = new DefaultExtContext<>(true, true);

        context.registerBusiness(new BusinessX());
        e = assertThrows(ExtensionException.class, () -> context.registerBusiness(new BusinessX()));
        assertEquals("business BusinessX already registered", e.getMessage());

        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());

        e = assertThrows(ExtensionException.class, () -> context.registerAbility(new AbilityL()));
        assertEquals("ability AbilityL should implement at least one interface that annotated with @ExtensionPoint", e.getMessage());

        context.registerAbility(new AbilityM());
        e = assertThrows(ExtensionException.class, () -> context.registerAbility(new AbilityM()));
        assertEquals("ability AbilityM already registered", e.getMessage());

        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());

        context.registerBusiness(new BusinessPriorityConflict());
    }

    @Test
    public void testValidateContext() throws Exception {
        ExtensionException e;
        DefaultExtContext<Object> context = new DefaultExtContext<>(false, false);
        context.validateContext();

        context.registerBusiness(new BusinessUnknownAbilityCode());
        e = assertThrows(ExtensionException.class, context::validateContext);
        assertEquals("ability Unknown not found", e.getMessage());

        context = new DefaultExtContext<>(false, false);
        context.registerAbility(new AbilityM());
        context.registerBusiness(new BusinessUsedAbilityCodeDuplicate());
        e = assertThrows(ExtensionException.class, context::validateContext);
        assertEquals("business BusinessUsedAbilityCodeDuplicate used ability code AbilityM duplicate", e.getMessage());

        context = new DefaultExtContext<>(false, false);
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerBusiness(new BusinessUsedAbilityPriorityDuplicate());
        e = assertThrows(ExtensionException.class, context::validateContext);
        assertEquals("business BusinessUsedAbilityPriorityDuplicate used ability priority 200 duplicate", e.getMessage());

        context = new DefaultExtContext<>(false, false);
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbilityInvalid());
        context.registerBusiness(new BusinessUsedAbilityPriority());
        e = assertThrows(ExtensionException.class, context::validateContext);
        assertEquals("default ability should implements all extension interface, but current default ability not implements extension [io.github.xiaoshicae.extension.core.ExtA]", e.getMessage());

        context = new DefaultExtContext<>(false, false);
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());
        context.registerBusiness(new BusinessUsedAbilityPriority());
    }

    @Test
    public void testInitSession() throws Exception {
        ExtensionException e;
        DefaultExtContext<Object> context = new DefaultExtContext<>(false, true);

        context.registerBusiness(new BusinessX());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());
        context.registerBusiness(new BusinessZZ());

        e = assertThrows(ExtensionException.class, () -> context.initSession("UnknownBiz"));
        assertEquals("business not found", e.getMessage());

        e = assertThrows(ExtensionException.class, () -> context.initSession("BusinessZZ"));
        assertEquals("multiple business found, matched business codes: [BusinessZ, BusinessZZ]", e.getMessage());

        e = assertThrows(ExtensionException.class, () -> context.initSession("BusinessX"));
        assertEquals("ability AbilityM not found", e.getMessage());

        context.registerAbility(new AbilityM());

        e = assertThrows(ExtensionException.class, () -> context.initSession("BusinessX"));
        assertEquals("ability ability.application.default not found", e.getMessage());

        context.registerAbility(new ExtDefaultAbility());

        context.initSession("BusinessX");

        context.validateContext();
    }

    @Test
    public void testGetExtension() throws Exception {
        List<ExtA> extAList;
        ExtA extA;
        DefaultExtContext<Object> context = new DefaultExtContext<>(false, false);

        context.registerBusiness(new BusinessX());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());
        context.registerBusiness(new BusinessZZ());
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());

        assertThrows(ExtensionException.class, () -> context.getAllMatchedExtension(ExtA.class));
        assertThrows(ExtensionException.class, () -> context.getFirstMatchedExtension(ExtA.class));

        context.initSession("XXX");
        extAList = context.getAllMatchedExtension(ExtA.class);
        assertEquals(1, extAList.size());
        extA = context.getFirstMatchedExtension(ExtA.class);
        assertEquals("ExtDefaultAbility do extA", extA.extA());

        context.initSession("BusinessX-BusinessY");
        extAList = context.getAllMatchedExtension(ExtA.class);
        assertEquals(2, extAList.size());
        assertEquals("BusinessX exec extA", extAList.get(0).extA());
        assertEquals("ExtDefaultAbility do extA", extAList.get(1).extA());
        extA = context.getFirstMatchedExtension(ExtA.class);
        assertEquals("BusinessX exec extA", extA.extA());

        context.removeSession();
        assertThrows(ExtensionException.class, () -> context.getAllMatchedExtension(ExtA.class));
    }
}
