package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirstMatchedExtPointProxyFactoryTest {

    @Test
    public void test() throws Exception {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Ext1.class);
        context.registerMatcherParamClass(MP.class);

        context.registerExtensionPointDefaultImplementation(AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new DefaultImpl1()));

        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new BizA()));

        FirstMatchedExtPointProxyFactory<Ext1> factory = new FirstMatchedExtPointProxyFactory<>(Ext1.class, context);

        context.initSession(new MP("bizA"));
        Ext1 proxy = factory.getProxy();
        assertEquals("BizA doSomething1", proxy.doSomething1());

        context.initSession(new MP("XXX"));
        proxy = factory.getProxy();
        assertEquals("DefaultImpl1 doSomething1", proxy.doSomething1());
    }
}

@ExtensionPointDefaultImplementation
class DefaultImpl1 implements Ext1 {
    @Override
    public String doSomething1() {
        return "DefaultImpl1 doSomething1";
    }
}

@Business(code = "bizA")
class BizA implements Matcher<MP>, Ext1 {

    @Override
    public String doSomething1() {
        return "BizA doSomething1";
    }

    @Override
    public Boolean match(MP param) {
        return param.name.equals("bizA");
    }
}