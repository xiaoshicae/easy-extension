package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import java.util.List;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtContextTest {

    @Test
    public void testRegisterExtensionPoint() throws Exception {
        RegisterException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        // param invalid
        e = assertThrows(RegisterException.class, () -> context.registerExtensionPoint(null));
        assertEquals("clazz should not be null", e.getMessage());

        class NotInterface {
        }

        Class<?> clazz = NotInterface.class;
        e = assertThrows(RegisterException.class, () -> context.registerExtensionPoint(clazz));
        assertEquals("clazz should be an interface type", e.getMessage());

        // duplicate register
        interface ExtensionPoint1 {
        }

        interface ExtensionPoint2 {
        }
        context.registerExtensionPoint(ExtensionPoint1.class);
        context.registerExtensionPoint(ExtensionPoint2.class);
        e = assertThrows(RegisterException.class, () -> context.registerExtensionPoint(ExtensionPoint1.class));
        assertEquals("class [" + ExtensionPoint1.class.getName() + "] already registered", e.getMessage());
    }

    @Test
    public void testRegisterMatcherParamClass() throws Exception {
        RegisterException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        e = assertThrows(RegisterException.class, () -> context.registerMatcherParamClass(null));
        assertEquals("matcher param class should not be null", e.getMessage());

        context.registerMatcherParamClass(Object.class);
        e = assertThrows(RegisterException.class, () -> context.registerMatcherParamClass(Object.class));
        assertEquals("matcher param class already registered", e.getMessage());

        Class<Object> matcherParamClass = context.getMatcherParamClass();
        assertNotNull(matcherParamClass);
    }

    @Test
    public void testRegisterExtensionPointDefaultImplementation() throws Exception {
        ExtensionException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        e = assertThrows(ExtensionException.class, () -> context.registerExtensionPointDefaultImplementation(null));
        assertEquals("extension point default implementation should not be null", e.getMessage());

        class DefaultExtensionPointImpl extends AbstractExtensionPointDefaultImplementation<Object> {
            @Override
            public List<Class<?>> implementExtensionPoints() {
                return List.of();
            }
        }
        context.registerExtensionPointDefaultImplementation(new DefaultExtensionPointImpl());

        DefaultExtensionPointImpl defaultExtension = new DefaultExtensionPointImpl();
        e = assertThrows(ExtensionException.class, () -> context.registerExtensionPointDefaultImplementation(defaultExtension));
        assertEquals("extension point default implementation already registered", e.getMessage());

        IExtensionPointGroupDefaultImplementation<Object> instance = context.getExtensionPointDefaultImplementation();
        assertNotNull(instance);
    }

    @Test
    public void testAbility() throws Exception {
        RegisterException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        context.registerExtensionPoint(ExtA.class);
        context.registerExtensionPoint(ExtB.class);
        context.registerExtensionPoint(ExtC.class);

        e = assertThrows(RegisterException.class, () -> context.registerAbility(null));
        assertEquals("ability should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> context.registerAbility(new AbilityL()));
        assertEquals("extension point [io.github.xiaoshicae.extension.core.ExtD] not registered", e.getMessage());

        e = assertThrows(RegisterException.class, () -> context.registerAbility(new AbilityM()));
        assertEquals("ability [AbilityM] should implement at least one extension point", e.getMessage());

        context.registerAbility(new AbilityN());
        e = assertThrows(RegisterException.class, () -> context.registerAbility(new AbilityN()));
        assertEquals("ability [AbilityN] already registered", e.getMessage());

        List<IAbility<Object>> abilities = context.listAllAbility();
        assertEquals(abilities.size(), 1);
        assertEquals(abilities.get(0).code(), "AbilityN");
    }

    @Test
    public void testBusiness() throws Exception {
        RegisterException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(true, true);

        context.registerExtensionPoint(ExtA.class);
        context.registerExtensionPoint(ExtB.class);
        context.registerExtensionPoint(ExtC.class);
        context.registerAbility(new AbilityN());
        context.registerAbility(new AbilityNN());

        e = assertThrows(RegisterException.class, () -> context.registerBusiness(null));
        assertEquals("business should not be null", e.getMessage());

        e = assertThrows(RegisterException.class, () -> context.registerBusiness(new BusinessX()));
        assertEquals("extension point [io.github.xiaoshicae.extension.core.ExtD] not registered", e.getMessage());

        e = assertThrows(RegisterException.class, () -> context.registerBusiness(new BusinessY()));
        assertEquals("business [BusinessY] used ability [Unknown] not found", e.getMessage());

        e = assertThrows(RegisterException.class, () -> context.registerBusiness(new BusinessZ()));
        assertEquals("business [BusinessZ] used ability [AbilityN] duplicate", e.getMessage());

        context.registerBusiness(new BusinessZZZ());
        List<IBusiness<Object>> businesses = context.listAllBusiness();
        assertEquals(businesses.size(), 1);
        assertEquals(businesses.get(0).code(), "BusinessZZZ");
    }

    @Test
    public void testInitSession() throws Exception {
        ExtensionException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(false, true);

        context.registerMatcherParamClass(Object.class);
        context.registerExtensionPointDefaultImplementation(new ExtensionPointDefaultImplementation());

        context.registerExtensionPoint(ExtA.class);
        context.registerExtensionPoint(ExtB.class);
        context.registerExtensionPoint(ExtC.class);
        context.registerAbility(new AbilityN());
        context.registerAbility(new AbilityNN());
        context.registerBusiness(new BusinessZZZ());
        context.registerBusiness(new BusinessC1());
        context.registerBusiness(new BusinessC2());

        e = assertThrows(ExtensionException.class, () -> context.initSession("UnknownBiz"));
        assertEquals("no business matched", e.getMessage());

        e = assertThrows(ExtensionException.class, () -> context.initSession("BusinessC"));
        assertEquals("multiple business found, matched business codes: [BusinessC1, BusinessC2]", e.getMessage());
    }

    @Test
    public void testGetMatchedExtension() throws Exception {
        ExtensionException e;
        DefaultExtensionContext<Object> context = new DefaultExtensionContext<>(false, true);

        context.registerMatcherParamClass(Object.class);
        context.registerExtensionPointDefaultImplementation(new ExtensionPointDefaultImplementation());

        context.registerExtensionPoint(ExtA.class);
        context.registerExtensionPoint(ExtB.class);
        context.registerExtensionPoint(ExtC.class);
        context.registerAbility(new AbilityN());
        context.registerAbility(new AbilityNN());
        context.registerBusiness(new BusinessZZZ());
        context.registerBusiness(new BusinessC1());
        context.registerBusiness(new BusinessC2());

        assertThrows(ExtensionException.class, () -> context.getAllMatchedExtension(ExtA.class));
        assertThrows(ExtensionException.class, () -> context.getFirstMatchedExtension(ExtA.class));

        context.initSession("XXX");
        List<ExtA> extAList = context.getAllMatchedExtension(ExtA.class);
        assertEquals(1, extAList.size());

        List<ExtB> extBList = context.getAllMatchedExtension(ExtB.class);
        assertEquals(3, extBList.size());

        context.removeSession();
        assertThrows(QueryException.class, () -> context.getAllMatchedExtension(ExtA.class));
    }
}
