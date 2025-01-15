package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;


public class ExtensionContextRegisterByAnnHelperTest {

    @Test
    public void testByNoAnn() throws ProxyException, RegisterException {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        ExtensionContextRegisterByAnnHelper<MP> helper = new ExtensionContextRegisterByAnnHelper<>(context);

        AbilityX abilityX = new AbilityX();
        helper.addAbilities(abilityX);
        helper.setMatcherParamClass(MP.class);
        BusinessA businessA = new BusinessA();
        helper.addBusinesses(businessA);
        DefaultImpl defaultImpl = new DefaultImpl();
        helper.setExtensionPointDefaultImplementation(defaultImpl);
        helper.addExtensionPointClasses(E1.class, E2.class);
        helper.doRegister();

        assertSame(MP.class, context.getMatcherParamClass());
        assertSame(defaultImpl, context.getExtensionPointDefaultImplementation());
        assertEquals(1, context.listAllAbility().size());
        assertSame(abilityX, context.listAllAbility().get(0));

        assertEquals(1, context.listAllBusiness().size());
        assertSame(businessA, context.listAllBusiness().get(0));

        assertEquals(2, context.listAllExtensionPoint().size());
        assertSame(E1.class, context.listAllExtensionPoint().get(0));
        assertSame(E2.class, context.listAllExtensionPoint().get(1));
    }

    @Test
    public void testByAnn() throws ProxyException, RegisterException {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        ExtensionContextRegisterByAnnHelper<MP> helper = new ExtensionContextRegisterByAnnHelper<>(context);

        helper.addExtensionPointClasses(E1.class, E2.class);

        helper.setMatcherParamClass(MP.class);

        InnerDefaultImpl defaultImpl = new InnerDefaultImpl();
        helper.setExtensionPointDefaultImplementation(defaultImpl);

        A1 a1 = new A1();
        helper.addAbilities(a1);

        IAbility<MP> a2 = new A2();
        helper.addAbilities(a2);

        B1 b1 = new B1();
        helper.addBusinesses(b1);

        B2 b2 = new B2();
        helper.addBusinesses(b2);

        helper.doRegister();

        assertSame(MP.class, context.getMatcherParamClass());
        assertNotSame(defaultImpl, context.getExtensionPointDefaultImplementation());
        assertEquals(2, context.listAllAbility().size());
        assertNotSame(a1, context.listAllAbility().get(0));
        assertSame(a2, context.listAllAbility().get(1));

        assertEquals(2, context.listAllBusiness().size());
        assertNotSame(b1, context.listAllBusiness().get(0));
        assertSame(b2, context.listAllBusiness().get(1));

        assertEquals(2, context.listAllExtensionPoint().size());
        assertSame(E1.class, context.listAllExtensionPoint().get(0));
        assertSame(E2.class, context.listAllExtensionPoint().get(1));
    }
}


@Ability(code = "a")
class A1 implements Matcher<MP>, E1 {
    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "a");
    }
}

class A2 implements IAbility<MP>, E1, E2 {

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(E2.class);
    }

    @Override
    public String code() {
        return "b";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "c");
    }
}

@Business(code = "b1")
class B1 implements Matcher<MP>, E1 {
    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "b1");
    }
}

class B2 implements IBusiness<MP> {
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public String code() {
        return "b3";
    }

    @Override
    public Boolean match(MP param) {
        return Objects.equals(param.name, "b3");
    }
}

@ExtensionPointDefaultImplementation
class InnerDefaultImpl implements E1, E2 {
}