package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AllMatchedExtPointProxyFactoryTest {
    @Test
    public void test() throws Exception {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Ext1.class);
        context.registerMatcherParamClass(MP.class);

        context.registerExtensionPointDefaultImplementation(AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new DefaultImpl2()));

        context.registerAbility(AnnProxyConvertUtils.convertAnnAbilityToProxy(new AbilityXXX()));

        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new BizB()));

        AllMatchedExtPointProxyFactory<Ext1> factory = new AllMatchedExtPointProxyFactory<>(Ext1.class, context);

        context.initSession(new MP("bizB-XXX"));
        List<Ext1> proxy = factory.getProxy();
        assertEquals("bizB doSomething1", proxy.get(0).doSomething1());
        assertEquals("abilityXXX doSomething1", proxy.get(1).doSomething1());
        assertEquals("DefaultImpl2 doSomething1", proxy.get(2).doSomething1());
    }
}

@ExtensionPointDefaultImplementation
class DefaultImpl2 implements Ext1 {
    @Override
    public String doSomething1() {
        return "DefaultImpl2 doSomething1";
    }
}

@Ability(code = "abilityXXX")
class AbilityXXX implements Matcher<MP>, Ext1 {

    @Override
    public String doSomething1() {
        return "abilityXXX doSomething1";
    }

    @Override
    public Boolean match(MP param) {
        return param.name.contains("XXX");
    }
}

@Business(code = "bizB", abilities = {"abilityXXX"})
class BizB implements Matcher<MP>, Ext1 {

    @Override
    public String doSomething1() {
        return "bizB doSomething1";
    }

    @Override
    public Boolean match(MP param) {
        return param.name.contains("bizB");
    }
}
