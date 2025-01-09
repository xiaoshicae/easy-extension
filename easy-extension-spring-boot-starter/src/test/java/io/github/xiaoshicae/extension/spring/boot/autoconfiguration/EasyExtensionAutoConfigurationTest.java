package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.MatcherParamHolder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class EasyExtensionAutoConfigurationTest {

    @Test
    public void test() throws RegisterException {
        EasyExtensionAutoConfiguration<MP> easyExtensionAutoConfiguration = new EasyExtensionAutoConfiguration<>();

        AbilityX abilityX = new AbilityX();
        easyExtensionAutoConfiguration.setAbilities(List.of(abilityX));
        easyExtensionAutoConfiguration.setMatcherParamHolder(new MatcherParamHolder<>(MP.class));
        BusinessA businessA = new BusinessA();
        easyExtensionAutoConfiguration.setBusinesses(List.of(businessA));
        DefaultImpl defaultImpl = new DefaultImpl();
        easyExtensionAutoConfiguration.setExtensionPointGroupImplementation(defaultImpl);
        easyExtensionAutoConfiguration.setExtensionPointHolders(List.of(new ExtensionPointHolder(Ext1.class), new ExtensionPointHolder(Ext2.class)));

        IExtensionContext<MP> context = easyExtensionAutoConfiguration.registerExtensionContext(new EasyExtensionConfigurationProperties());

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