package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ExtensionContextRegisterHelperTest<T> {


    @Test
    public void test() throws RegisterException {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        ExtensionContextRegisterHelper<MP> helper = new ExtensionContextRegisterHelper<>(context);

        AbilityX abilityX = new AbilityX();
        helper.addAbilities(abilityX);
        helper.setMatcherParamClass(MP.class);
        BusinessA businessA = new BusinessA();
        helper.addBusinesses(businessA);
        DefaultImpl defaultImpl = new DefaultImpl();
        helper.setExtensionPointDefaultImplementation(defaultImpl);
        helper.addExtensionPointClasses(Ext1.class, Ext2.class);
        helper.doRegister();

        assertSame(MP.class, context.getMatcherParamClass());
        assertSame(defaultImpl, context.getExtensionPointDefaultImplementation());
        assertEquals(1, context.listAllAbility().size());
        assertSame(abilityX, context.listAllAbility().get(0));

        assertEquals(1, context.listAllBusiness().size());
        assertSame(businessA, context.listAllBusiness().get(0));

        assertEquals(2, context.listAllExtensionPoint().size());
        assertSame(Ext1.class, context.listAllExtensionPoint().get(0));
        assertSame(Ext2.class, context.listAllExtensionPoint().get(1));
    }
}


class MP {
}

interface Ext1 {
}

interface Ext2 {
}

class AbilityX extends AbstractAbility<MP> implements Ext1, Ext2 {
    @Override
    public String code() {
        return "AbilityX";
    }

    @Override
    public Boolean match(MP param) {
        return false;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class);
    }
}

class BusinessA extends AbstractBusiness<MP> implements Ext1, Ext2 {
    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    @Override
    public String code() {
        return "BusinessA";
    }

    @Override
    public Boolean match(MP param) {
        return null;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class, Ext2.class);
    }
}

class DefaultImpl extends AbstractExtensionPointDefaultImplementation<MP> implements Ext1, Ext2 {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class, Ext2.class);
    }
}