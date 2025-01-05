package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionContextRegisterHelper;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MatchedExtensionDynamicProxyFactoryTest {
    private static final IExtensionContext<MyParam> extensionContext = new DefaultExtensionContext<>(true, false);

    @BeforeAll
    public static void setUp() throws Exception {
        ExtensionContextRegisterHelper<MyParam> helper = new ExtensionContextRegisterHelper<>();
        helper.addAbilities(new AbilityA(), new AbilityB()).
                setMatcherParamClass(MyParam.class).
                addExtensionPointClasses(ExtA.class, ExtB.class).
                setExtensionPointDefaultImplementation(new DefaultImpl()).addBusinesses(new BizA(), new BizB());
        helper.doRegister(extensionContext);
    }

    @Test
    public void testNewFirstMatchedInstanceExtA() throws SessionException {
        MatchedExtensionDynamicProxyFactory<ExtA> factoryA = new MatchedExtensionDynamicProxyFactory<>(ExtA.class);
        factoryA.setExtensionFactory(extensionContext);
        ExtA extAProxy = factoryA.newFirstMatchedInstance();

        // nothing matched, DefaultImpl fallback
        extensionContext.initSession(new MyParam("a"));
        assertEquals("DefaultImpl a", extAProxy.a());
        extensionContext.removeSession();

        // match BizA, AbilityA, AbilityB, DefaultImpl
        // BizA has max priority
        extensionContext.initSession(new MyParam("biza"));
        assertEquals("BizA a", extAProxy.a());
        extensionContext.removeSession();

        // match BizB, AbilityB, DefaultImpl
        // BizB not implements ExtA, AbilityB implements ExtA and priority bigger than DefaultImpl
        extensionContext.initSession(new MyParam("bizb"));
        assertEquals("AbilityB a", extAProxy.a());
        extensionContext.removeSession();


        // match BizB, AbilityB, DefaultImpl
        // AbilityB has max priority
        extensionContext.initSession(new MyParam("bizb"));
        assertEquals("AbilityB a", extAProxy.a());
        extensionContext.removeSession();
    }

    @Test
    public void testNewFirstMatchedInstanceExtB() throws SessionException {
        MatchedExtensionDynamicProxyFactory<ExtB> factoryB = new MatchedExtensionDynamicProxyFactory<>(ExtB.class);
        factoryB.setExtensionFactory(extensionContext);
        ExtB extBProxy = factoryB.newFirstMatchedInstance();

        // nothing matched, DefaultImpl fallback
        extensionContext.initSession(new MyParam("a"));
        assertEquals("DefaultImpl b", extBProxy.b());
        extensionContext.removeSession();

        // match BizA, AbilityA, AbilityB, DefaultImpl
        // BizA has max priority
        extensionContext.initSession(new MyParam("biza"));
        assertEquals("BizA b", extBProxy.b());
        extensionContext.removeSession();

        // match AbilityA, BizB, AbilityB, DefaultImpl
        // AbilityA has max priority, but not implements ExtB
        // so matched AbilityB
        extensionContext.initSession(new MyParam("bizb"));
        assertEquals("AbilityB b", extBProxy.b());
        extensionContext.removeSession();
    }

    @Test
    public void testNewAllMatchedInstanceExtA() throws SessionException {
        MatchedExtensionDynamicProxyFactory<ExtA> factoryA = new MatchedExtensionDynamicProxyFactory<>(ExtA.class);
        factoryA.setExtensionFactory(extensionContext);
        List<ExtA> extAListProxy = factoryA.newAllMatchedInstance();

        // nothing matched, DefaultImpl fallback
        extensionContext.initSession(new MyParam("a"));
        List<String> res = new ArrayList<>();
        for (ExtA extA : extAListProxy) {
            res.add(extA.a());
        }
        assertArrayEquals(new String[]{"DefaultImpl a"}, res.toArray());
        extensionContext.removeSession();

        // match BizA, AbilityA, AbilityB, DefaultImpl
        // BizA has max priority
        extensionContext.initSession(new MyParam("biza"));
        res = new ArrayList<>();
        for (ExtA extA : extAListProxy) {
            res.add(extA.a());
        }
        assertArrayEquals(new String[]{"BizA a", "AbilityA a", "AbilityB a", "DefaultImpl a"}, res.toArray());
        extensionContext.removeSession();

        // match BizB, AbilityB, DefaultImpl
        // BizB not implements ExtA, AbilityB implements ExtA and priority bigger than DefaultImpl
        extensionContext.initSession(new MyParam("bizb"));
        res = new ArrayList<>();
        for (ExtA extA : extAListProxy) {
            res.add(extA.a());
        }
        assertArrayEquals(new String[]{"AbilityB a", "DefaultImpl a"}, res.toArray());
        extensionContext.removeSession();


        // match BizB, AbilityB, DefaultImpl
        // AbilityB has max priority
        extensionContext.initSession(new MyParam("bizb"));
        res = new ArrayList<>();
        for (ExtA extA : extAListProxy) {
            res.add(extA.a());
        }
        assertArrayEquals(new String[]{"AbilityB a", "DefaultImpl a"}, res.toArray());
        extensionContext.removeSession();
    }

    @Test
    public void testNewAllMatchedInstanceExtB() throws SessionException {
        MatchedExtensionDynamicProxyFactory<ExtB> factoryB = new MatchedExtensionDynamicProxyFactory<>(ExtB.class);
        factoryB.setExtensionFactory(extensionContext);
        List<ExtB> extBListProxy = factoryB.newAllMatchedInstance();

        extensionContext.initSession(new MyParam("a"));
        List<String> res = new ArrayList<>();
        for (ExtB extB : extBListProxy) {
            res.add(extB.b());
        }
        assertArrayEquals(new String[]{"DefaultImpl b"}, res.toArray());
        extensionContext.removeSession();

        // match BizA, AbilityA, AbilityB, DefaultImpl
        // BizA has max priority
        // AbilityA not implement ExtB
        extensionContext.initSession(new MyParam("biza"));
        res = new ArrayList<>();
        for (ExtB extB : extBListProxy) {
            res.add(extB.b());
        }
        assertArrayEquals(new String[]{"BizA b", "AbilityB b", "DefaultImpl b"}, res.toArray());
        extensionContext.removeSession();

        // match AbilityA, BizB, AbilityB, DefaultImpl
        // AbilityA has max priority, but not implements ExtB
        // so matched AbilityB
        extensionContext.initSession(new MyParam("bizb"));
        res = new ArrayList<>();
        for (ExtB extB : extBListProxy) {
            res.add(extB.b());
        }
        assertArrayEquals(new String[]{"AbilityB b", "BizB b", "DefaultImpl b"}, res.toArray());
        extensionContext.removeSession();
    }
}
