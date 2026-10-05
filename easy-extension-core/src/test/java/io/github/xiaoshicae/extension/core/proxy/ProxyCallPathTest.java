package io.github.xiaoshicae.extension.core.proxy;

import io.github.xiaoshicae.extension.core.AllMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.FirstMatchedExtPointProxyFactory;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterByAnnHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the caller actually sees when calling an extension point through the injected proxy
 * or through {@code context.invoke}: exceptions thrown by the implementation must come out as-is,
 * the proxy must not need a session for {@code Object} methods, and the instance registered for
 * an extension point must be the user's real object, not the framework descriptor proxy.
 */
public class ProxyCallPathTest {

    @MatcherParam
    public static class PayParam {
        final String tenant;

        public PayParam(String tenant) {
            this.tenant = tenant;
        }
    }

    @ExtensionPoint
    public interface Pay {
        String pay();

        void fail() throws IOException;
    }

    @ExtensionPointDefaultImplementation
    public static class DefaultPay implements Pay {
        @Override
        public String pay() {
            return "default";
        }

        @Override
        public void fail() throws IOException {
            throw new IOException("default io");
        }
    }

    @Business(code = "b1")
    public static class PayBusiness implements Matcher<PayParam>, Pay {
        @Override
        public boolean match(PayParam param) {
            return "t1".equals(param.tenant);
        }

        @Override
        public String pay() {
            throw new IllegalStateException("boom");
        }

        @Override
        public void fail() throws IOException {
            throw new IOException("biz io");
        }
    }

    private DefaultExtensionContext<PayParam> context;
    private Pay proxy;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        context = new DefaultExtensionContext<>();
        new ExtensionContextRegisterByAnnHelper<PayParam>(context)
                .addExtensionPointClasses(Pay.class)
                .setMatcherParamClass(PayParam.class)
                .setExtensionPointDefaultImplementation(new DefaultPay())
                .addBusinesses(new PayBusiness())
                .doRegister();
        proxy = new FirstMatchedExtPointProxyFactory<>(Pay.class, context).getProxy();
    }

    @Test
    public void testProxyPropagatesExtensionException() throws Exception {
        context.initSession(new PayParam("t1"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> proxy.pay());
        assertEquals("boom", e.getMessage());

        // declared checked exceptions come out as themselves too
        IOException io = assertThrows(IOException.class, () -> proxy.fail());
        assertEquals("biz io", io.getMessage());
    }

    @Test
    public void testContextInvokePropagatesExtensionException() throws Exception {
        context.initSession(new PayParam("t1"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> context.invoke(Pay.class, Pay::pay));
        assertEquals("boom", e.getMessage());
    }

    @Test
    public void testProxyWithoutSessionThrowsInvokeException() {
        context.removeSession();

        InvokeException e = assertThrows(InvokeException.class, () -> proxy.pay());
        assertInstanceOf(QueryException.class, e.getCause());
        assertEquals("call Pay.pay failed, " + e.getCause().getMessage(), e.getMessage());
    }

    @Test
    public void testProxyObjectMethodsDoNotNeedSession() {
        context.removeSession();

        assertEquals("FirstMatchedProxy[" + Pay.class.getName() + "]", proxy.toString());
        assertEquals(System.identityHashCode(proxy), proxy.hashCode());
        assertEquals(proxy, proxy);
        assertNotEquals(proxy, new FirstMatchedExtPointProxyFactory<>(Pay.class, context).getProxy());
    }

    @Test
    public void testAllMatchedProxy() throws Exception {
        List<Pay> all = new AllMatchedExtPointProxyFactory<>(Pay.class, context).getProxy();

        // without a session: toString degrades to a description, everything else is a clean InvokeException
        context.removeSession();
        assertEquals("AllMatchedProxy[" + Pay.class.getName() + "]", all.toString());
        InvokeException e = assertThrows(InvokeException.class, all::size);
        assertInstanceOf(QueryException.class, e.getCause());
        assertEquals("call Pay.size failed, " + e.getCause().getMessage(), e.getMessage());
        assertThrows(InvokeException.class, all::hashCode);

        // with a session: it behaves like the resolved list, including the List equals/hashCode contract
        context.initSession(new PayParam("t1"));
        List<Pay> resolved = context.getAllMatchedExtension(Pay.class);
        assertEquals(2, all.size());
        assertEquals(resolved, all);
        assertEquals(resolved.hashCode(), all.hashCode());
        assertEquals(resolved.toString(), all.toString());
    }

    @Test
    public void testRegisteredInstanceIsTheRealObject() throws Exception {
        context.initSession(new PayParam("t1"));
        Pay first = context.getFirstMatchedExtension(Pay.class);
        assertInstanceOf(PayBusiness.class, first);
        assertFalse(first instanceof IProxy);

        context.initSession(new PayParam("t2"));
        assertInstanceOf(DefaultPay.class, context.getFirstMatchedExtension(Pay.class));
    }

    @Test
    public void testDescriptorStillExposesRealInstance() throws Exception {
        context.initSession(new PayParam("t1"));
        IBusiness<PayParam> descriptor = context.listAllBusiness().get(0);

        assertEquals("b1", descriptor.code());
        assertInstanceOf(IProxy.class, descriptor);
        assertSame(context.getFirstMatchedExtension(Pay.class), ((IProxy<?>) descriptor).getInstance());

        // calling an extension method on the descriptor itself also yields the original exception
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> ((Pay) descriptor).pay());
        assertEquals("boom", e.getMessage());
    }
}
