package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.AllMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.FirstMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a caller sees when an extension implementation (or the lookup in front of it) fails.
 *
 * <p>The proxies the framework hands out are layered: the injected {@code FirstMatched} / {@code AllMatched}
 * proxy, then the proxy around the business / ability / default implementation, then the implementation. Every
 * layer used to call {@code Method.invoke} and let {@code InvocationTargetException} escape, which the JDK proxy
 * then wrapped in {@code UndeclaredThrowableException}: a plain {@code IllegalStateException} reached the caller
 * as four nested wrappers, and a declared {@code IOException} could not be caught as such at all.</p>
 */
public class ProxyExceptionTransparencyTest {

    private static final String LEGACY_SWITCH = "easy-extension.legacy-exception-wrapping";

    /** An extension point whose methods fail in each way a Java method can. */
    @ExtensionPoint
    public interface Faulty {
        String ok();

        String runtimeFailure();

        String checkedFailure() throws IOException;

        String errorFailure();
    }

    @ExtensionPointDefaultImplementation
    public static class FaultyDefault implements Faulty {
        @Override
        public String ok() {
            return "default ok";
        }

        @Override
        public String runtimeFailure() {
            throw new IllegalStateException("default runtime failure");
        }

        @Override
        public String checkedFailure() throws IOException {
            throw new IOException("default io failure");
        }

        @Override
        public String errorFailure() {
            throw new AssertionError("default error");
        }
    }

    @Business(code = "biz.faulty")
    public static class FaultyBusiness implements Matcher<MP>, Faulty {
        @Override
        public boolean match(MP param) {
            return "faulty".equals(param.name);
        }

        @Override
        public String ok() {
            return "business ok";
        }

        @Override
        public String runtimeFailure() {
            throw new IllegalStateException("business runtime failure");
        }

        @Override
        public String checkedFailure() throws IOException {
            throw new IOException("business io failure");
        }

        @Override
        public String errorFailure() {
            throw new AssertionError("business error");
        }
    }

    private static IExtensionContext<MP> newContext() throws Exception {
        IExtensionContext<MP> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Faulty.class);
        context.registerMatcherParamClass(MP.class);
        context.registerExtensionPointDefaultImplementation(
                AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new FaultyDefault()));
        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(new FaultyBusiness()));
        return context;
    }

    private static void assertFailuresReachTheCallerUnchanged(Faulty faulty, String origin) {
        IllegalStateException runtime = assertThrows(IllegalStateException.class, faulty::runtimeFailure);
        assertEquals(origin + " runtime failure", runtime.getMessage());
        assertNull(runtime.getCause(), "the original exception must not be wrapped");

        IOException checked = assertThrows(IOException.class, faulty::checkedFailure);
        assertEquals(origin + " io failure", checked.getMessage());
        assertNull(checked.getCause(), "the original exception must not be wrapped");

        AssertionError error = assertThrows(AssertionError.class, faulty::errorFailure);
        assertEquals(origin + " error", error.getMessage());
        assertNull(error.getCause(), "the original error must not be wrapped");
    }

    @Test
    public void testBusinessProxyRethrowsImplementationFailuresUnchanged() throws Exception {
        Faulty business = (Faulty) AnnProxyConvertUtils.convertAnnBusinessToProxy(new FaultyBusiness());

        assertEquals("business ok", business.ok());
        assertFailuresReachTheCallerUnchanged(business, "business");
    }

    @Test
    public void testDefaultImplementationProxyRethrowsImplementationFailuresUnchanged() throws Exception {
        Faulty defaultImpl = (Faulty) AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new FaultyDefault());

        assertEquals("default ok", defaultImpl.ok());
        assertFailuresReachTheCallerUnchanged(defaultImpl, "default");
    }

    @Test
    public void testFirstMatchedProxyRethrowsBusinessFailuresUnchanged() throws Exception {
        IExtensionContext<MP> context = newContext();
        Faulty proxy = new FirstMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

        context.initSession(new MP("faulty"));
        try {
            assertEquals("business ok", proxy.ok());
            assertFailuresReachTheCallerUnchanged(proxy, "business");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testFirstMatchedProxyRethrowsDefaultImplementationFailuresUnchanged() throws Exception {
        IExtensionContext<MP> context = newContext();
        Faulty proxy = new FirstMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

        context.initSession(new MP("someone else"));
        try {
            assertEquals("default ok", proxy.ok());
            assertFailuresReachTheCallerUnchanged(proxy, "default");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testAllMatchedProxyRethrowsFailuresOfTheListAndOfItsElementsUnchanged() throws Exception {
        IExtensionContext<MP> context = newContext();
        List<Faulty> proxy = new AllMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

        context.initSession(new MP("faulty"));
        try {
            assertEquals(2, proxy.size(), "business and default implementation");
            assertFailuresReachTheCallerUnchanged(proxy.get(0), "business");
            assertFailuresReachTheCallerUnchanged(proxy.get(1), "default");

            IndexOutOfBoundsException outOfBounds = assertThrows(IndexOutOfBoundsException.class, () -> proxy.get(5));
            assertTrue(outOfBounds.getMessage().contains("5"), outOfBounds.getMessage());
            assertNull(outOfBounds.getCause(), "the original exception must not be wrapped");
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testFirstMatchedProxyReportsMissingSessionAsInvokeException() throws Exception {
        IExtensionContext<MP> context = newContext();
        Faulty proxy = new FirstMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

        InvokeException e = assertThrows(InvokeException.class, proxy::ok);
        QueryNotFoundException cause = assertInstanceOf(QueryNotFoundException.class, e.getCause());
        assertTrue(cause.getMessage().endsWith("may be session not init"), cause.getMessage());
        assertEquals("invoke Faulty#ok failed, " + cause.getMessage(), e.getMessage());
    }

    @Test
    public void testAllMatchedProxyReportsMissingSessionAsInvokeException() throws Exception {
        IExtensionContext<MP> context = newContext();
        List<Faulty> proxy = new AllMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

        InvokeException e = assertThrows(InvokeException.class, proxy::size);
        QueryNotFoundException cause = assertInstanceOf(QueryNotFoundException.class, e.getCause());
        assertTrue(cause.getMessage().endsWith("may be session not init"), cause.getMessage());
        assertEquals("invoke List<Faulty>#size failed, " + cause.getMessage(), e.getMessage());
    }

    @Test
    public void testLegacySwitchRestoresTheOldWrapping() throws Exception {
        System.setProperty(LEGACY_SWITCH, "true");
        try {
            // the switch is read when a proxy is created, so everything is built after it is set
            IExtensionContext<MP> context = newContext();
            Faulty proxy = new FirstMatchedExtPointProxyFactory<>(Faulty.class, context).getProxy();

            UndeclaredThrowableException noSession = assertThrows(UndeclaredThrowableException.class, proxy::ok);
            assertInstanceOf(QueryNotFoundException.class, noSession.getCause());

            context.initSession(new MP("faulty"));
            try {
                assertEquals("business ok", proxy.ok());

                UndeclaredThrowableException wrapped = assertThrows(UndeclaredThrowableException.class, proxy::runtimeFailure);
                assertInstanceOf(InvocationTargetException.class, wrapped.getCause());

                UndeclaredThrowableException wrappedChecked = assertThrows(UndeclaredThrowableException.class, proxy::checkedFailure);
                assertInstanceOf(InvocationTargetException.class, wrappedChecked.getCause());
            } finally {
                context.removeSession();
            }
        } finally {
            System.clearProperty(LEGACY_SWITCH);
        }
    }
}
