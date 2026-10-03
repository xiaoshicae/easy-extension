package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Before 4.0 only the interfaces a class declares itself counted as the extension points it implements. Those it
 * inherits (from a superclass, from a super-interface) count now, which is how application code usually shares
 * behavior, but they must not stop an application from starting when they belong to a module that is not scanned.
 */
public class InheritedExtensionPointRegistrationTest {

    @ExtensionPoint
    public interface Price {
        String price();
    }

    @ExtensionPoint
    public interface Discount {
        String discount();
    }

    @ExtensionPoint
    public interface Base {
        String base();
    }

    @ExtensionPoint
    public interface Sub extends Base {
        String sub();
    }

    /** An extension point nobody registers, as if it lived in a module that is not scanned. */
    @ExtensionPoint
    public interface Outside {
        String outside();
    }

    public static class Req {
        final String name;

        public Req(String name) {
            this.name = name;
        }
    }

    /** A base class that implements an extension point to share its behavior. */
    public abstract static class DiscountingBase implements Matcher<Req>, Discount {
        @Override
        public String discount() {
            return "base discount";
        }
    }

    @Business(code = "vip")
    public static class Vip extends DiscountingBase implements Price {
        @Override
        public boolean match(Req param) {
            return "vip".equals(param.name);
        }

        @Override
        public String price() {
            return "vip price";
        }
    }

    @Business(code = "sub")
    public static class SubBusiness implements Matcher<Req>, Sub {
        @Override
        public boolean match(Req param) {
            return "sub".equals(param.name);
        }

        @Override
        public String sub() {
            return "sub";
        }

        @Override
        public String base() {
            return "base of sub";
        }
    }

    @Business(code = "outsider")
    public static class Outsider implements Matcher<Req>, Price, Outside {
        @Override
        public boolean match(Req param) {
            return "outsider".equals(param.name);
        }

        @Override
        public String price() {
            return "outsider price";
        }

        @Override
        public String outside() {
            return "outside";
        }
    }

    @ExtensionPointDefaultImplementation
    public static class AllDefaults implements Price, Discount {
        @Override
        public String price() {
            return "default price";
        }

        @Override
        public String discount() {
            return "default discount";
        }
    }

    @ExtensionPointDefaultImplementation
    public static class DefaultsWithOutside implements Price, Discount, Outside {
        @Override
        public String price() {
            return "default price";
        }

        @Override
        public String discount() {
            return "default discount";
        }

        @Override
        public String outside() {
            return "default outside";
        }
    }

    private static DefaultExtensionContext<Req> context(Class<?>... extensionPoints) throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        for (Class<?> extensionPoint : extensionPoints) {
            context.registerExtensionPoint(extensionPoint);
        }
        context.registerMatcherParamClass(Req.class);
        return context;
    }

    @Test
    public void testBusinessServesTheExtensionPointsItsSuperclassImplements() throws Exception {
        DefaultExtensionContext<Req> context = context(Price.class, Discount.class);
        context.addExtensionPointDefaultImplementation(AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new AllDefaults()));
        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new Vip()));
        context.validateRegistration();

        context.initSession(new Req("vip"));
        try {
            assertEquals("vip price", context.invoke(Price.class, Price::price));
            assertEquals("base discount", context.invoke(Discount.class, Discount::discount),
                    "the business implements Discount through its superclass: before 4.0 the default answered for it");
        } finally {
            context.removeSession();
        }

        context.initSession(new Req("somebody"));
        try {
            assertEquals("default discount", context.invoke(Discount.class, Discount::discount));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testInheritedExtensionPointNobodyRegisteredIsLeftOut() throws Exception {
        // Sub extends Base, but only Sub is registered
        DefaultExtensionContext<Req> context = context(Sub.class);

        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new SubBusiness()));

        context.initSession(new Req("sub"));
        try {
            assertEquals("sub", context.invoke(Sub.class, Sub::sub));
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testExtensionPointTheClassDeclaresItselfMustBeRegistered() throws Exception {
        DefaultExtensionContext<Req> context = context(Price.class);

        RegisterException e = assertThrows(RegisterException.class,
                () -> context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new Outsider())));

        assertEquals("extension point [" + Outside.class.getName() + "] not registered", e.getMessage());
    }

    @Test
    public void testDefaultImplementationFoundByAnnotationMayImplementAnUnregisteredExtensionPoint() throws Exception {
        DefaultExtensionContext<Req> context = context(Price.class, Discount.class);

        context.addExtensionPointDefaultImplementation(AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new DefaultsWithOutside()));
        context.validateRegistration();

        context.initSession(new Req("nobody"));
        try {
            assertEquals("default price", context.invoke(Price.class, Price::price));
            assertEquals("default discount", context.invoke(Discount.class, Discount::discount));
        } finally {
            context.removeSession();
        }
    }
}
