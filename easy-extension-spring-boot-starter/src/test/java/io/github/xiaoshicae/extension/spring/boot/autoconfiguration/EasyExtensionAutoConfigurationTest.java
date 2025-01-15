package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.core.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionAutoConfiguration;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.EasyExtensionConfigurationProperties;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ClassHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import org.junit.jupiter.api.Test;

import java.util.List;

public class EasyExtensionAutoConfigurationTest {

    @Test
    public void test() throws RegisterException, ProxyException {
        EasyExtensionAutoConfiguration<MP> easyExtensionAutoConfiguration = new EasyExtensionAutoConfiguration<>();

        AbilityX abilityX = new AbilityX();
        easyExtensionAutoConfiguration.setAbilities(List.of(abilityX));
        BusinessA businessA = new BusinessA();
        easyExtensionAutoConfiguration.setBusinesses(List.of(businessA));
        DefaultImpl defaultImpl = new DefaultImpl();
        easyExtensionAutoConfiguration.setExtensionPointGroupImplementation(defaultImpl);
        easyExtensionAutoConfiguration.setExtensionPointHolders(List.of(new ExtensionPointHolder(Ext1.class), new ExtensionPointHolder(Ext2.class)));
        easyExtensionAutoConfiguration.setClassHolders(List.of(new ClassHolder(MP.class)));

        easyExtensionAutoConfiguration.registerExtensionContext(new EasyExtensionConfigurationProperties());
    }
}


@MatcherParam
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