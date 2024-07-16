package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;


import io.github.xiaoshicae.extension.core.BaseDefaultAbility;
import io.github.xiaoshicae.extension.core.DefaultExtContext;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.AllMatchedExtensionProxy;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.ExtensionProxyFactory;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.FirstMatchedExtensionProxy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;


import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


public class ExtensionProxyTest {

    @Test
    public void test() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>();
        context.registerAbility(new DefaultAbility());
        context.initSession(null);
        FirstMatchedExtensionProxy<ExtA> proxy = new FirstMatchedExtensionProxy<>(ExtA.class, context);
        ExtA extA = (ExtA) Proxy.newProxyInstance(ExtA.class.getClassLoader(), new Class[]{ExtA.class}, proxy);
        String s = extA.doSomething();
        assertEquals("DefaultAbility do something", s);
    }

    @Test
    public void testAllMatchedExt() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>();
        context.registerAbility(new DefaultAbility());
        context.initSession(null);
        AllMatchedExtensionProxy<ExtA> proxy = new AllMatchedExtensionProxy<>(ExtA.class, context);
        List<ExtA> extAList = (List<ExtA>) Proxy.newProxyInstance(ArrayList.class.getClassLoader(), ArrayList.class.getInterfaces(), proxy);
        System.out.println(extAList.size());
        String s = extAList.get(0).doSomething();
        System.out.println(s);
        for (ExtA extA : extAList) {
            System.out.println(extA.doSomething());
        }
    }

    @Test
    public void testExtProxyFactory() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>();
        context.registerAbility(new DefaultAbility());
        context.initSession(null);
        ExtensionProxyFactory<ExtA> factory = new ExtensionProxyFactory<>( ExtA.class);
        factory.setExtFactory(context);
        String s = factory.newFirstMatchedInstance().doSomething();
        List<ExtA> extAList = factory.newAllMatchedInstance();
        System.out.println(s);
        for (ExtA extA : extAList) {
            System.out.println(extA.doSomething());
        }
    }
}

class DefaultAbility extends BaseDefaultAbility<Object> implements ExtA {
    @Override
    public String doSomething() {
        return "DefaultAbility do something";
    }
}
