package io.github.xiaoshicae.extension.proxy.util;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class CGLibProxyUtilTest {
    @Test
    public void testA() throws NoSuchMethodException {
        CGLibProxyDefinition<IA> d = new CGLibProxyDefinition<>();
        d.setInstance(new IAProxy());
        d.setImplInterfaces(IA.class);
        d.setSuperClass(X.class);
        d.setMustInvokeSuperMethods(IA.class.getDeclaredMethod("a", String.class), IA.class.getDeclaredMethod("b"));

        IA ia = CGLibProxyUtil.newCGLibProxy(d);
        assertEquals(ia.a("SS"), "SS X a");
        assertEquals(ia.b(), "X b");
        assertEquals(ia.c(), "IAProxy c");
        assertEquals(ia.d(), "IAProxy d");

        X a = (X) ia;
        assertEquals(a.e(), "X e");
    }

    @Test
    public void testB() throws NoSuchMethodException {
        CGLibProxyDefinition<IB> d = new CGLibProxyDefinition<>();
        d.setInstance(new IBProxy());
        d.setImplInterfaces(IB.class);
        d.setSuperClass(X.class);
        d.setMustInvokeSuperMethods(IA.class.getDeclaredMethod("b"));

        IB ib = CGLibProxyUtil.newCGLibProxy(d);

        X b = (X) ib;
        assertEquals("xxx X a", b.a("xxx"));
        assertEquals("X b", b.b());
        assertEquals("X c", b.c());
        assertEquals("X d", b.d());
        assertEquals("X e", b.e());
    }
}

interface IA {
    String a(String s);

    String b();

    String c();

    String d();
}

class X {

    public String a(String s) {
        return s + " X a";
    }

    public String b() {
        return "X b";
    }

    public String c() {
        return "X c";
    }

    public String d() {
        return "X d";
    }

    public String e() {
        return "X e";
    }
}

class IAProxy implements IA {
    @Override
    public String a(String s) {
        return "IAProxy a " + s;
    }

    @Override
    public String b() {
        return "IAProxy b";
    }

    @Override
    public String c() {
        return "IAProxy c";
    }

    @Override
    public String d() {
        return "IAProxy d";
    }
}

interface IB {
}

class IBProxy implements IB {
    public String a(String s) {
        return "IBProxy a " + s;
    }
}
