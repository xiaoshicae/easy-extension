package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.session.ExtensionSessionScope;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Session lifecycle edge cases of {@link DefaultExtensionContext}: misconfigured context,
 * several scopes on one thread, failed initialization.
 */
public class SessionRobustnessTest {

    @MatcherParam
    public static class Param {
        final String tenant;

        public Param(String tenant) {
            this.tenant = tenant;
        }
    }

    @ExtensionPoint
    public interface Pay {
        String pay();
    }

    public static class DefaultPay extends AbstractExtensionPointDefaultImplementation<Param> implements Pay {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pay.class);
        }

        @Override
        public String pay() {
            return "default";
        }
    }

    public static class TenantBusiness extends AbstractBusiness<Param> implements Pay {
        private final String code;
        private final String tenant;
        private final Integer priority;
        private final List<UsedAbility> usedAbilities;

        TenantBusiness(String code, String tenant, Integer priority, List<UsedAbility> usedAbilities) {
            this.code = code;
            this.tenant = tenant;
            this.priority = priority;
            this.usedAbilities = usedAbilities;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public boolean match(Param param) {
            return tenant.equals(param.tenant);
        }

        @Override
        public Integer priority() {
            return priority;
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            return usedAbilities;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pay.class);
        }

        @Override
        public String pay() {
            return "business " + code;
        }
    }

    private static DefaultExtensionContext<Param> context(boolean withDefaultImpl, TenantBusiness... businesses) throws Exception {
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Pay.class);
        context.registerMatcherParamClass(Param.class);
        if (withDefaultImpl) {
            context.registerExtensionPointDefaultImplementation(new DefaultPay());
        }
        for (TenantBusiness business : businesses) {
            context.registerBusiness(business);
        }
        return context;
    }

    @Test
    public void testInitSessionWithoutDefaultImplementation() throws Exception {
        DefaultExtensionContext<Param> context = context(false);

        SessionException e = assertThrows(SessionException.class, () -> context.initSession(new Param("t1")));
        assertEquals("extension point default implementation not registered", e.getMessage());
    }

    @Test
    public void testExplainScopedForEachScope() throws Exception {
        DefaultExtensionContext<Param> context = context(true, new TenantBusiness("b1", "t1", 0, List.of()));

        context.initSession("A", new Param("t1"));
        context.initSession("B", new Param("t2"));

        ExtensionExplanation<Pay> a = context.explainScoped("A", Pay.class);
        assertNotNull(a.selected());
        assertEquals("b1", a.selected().code());

        ExtensionExplanation<Pay> b = context.explainScoped("B", Pay.class);
        assertNotNull(b.selected());
        assertEquals(AbstractExtensionPointDefaultImplementation.DEFAULT_CODE, b.selected().code());

        // the "last" trace is still the most recently initialized scope
        assertEquals("B", context.getLastResolveTrace().getScope());
    }

    @Test
    public void testFailedInitSessionLeavesNoSession() throws Exception {
        // the business priority collides with the default implementation priority (Integer.MAX_VALUE),
        // which is only detectable when the session is built
        DefaultExtensionContext<Param> context = context(true,
                new TenantBusiness("b1", "t1", Integer.MAX_VALUE, List.of()));

        assertThrows(SessionException.class, () -> context.initSession(new Param("t1")));

        // neither a half-written session nor a stale one may survive a failed init
        assertThrows(QueryException.class, () -> context.getFirstMatchedExtension(Pay.class));
    }

    @Test
    public void testBusinessWithoutUsedAbilities() throws Exception {
        DefaultExtensionContext<Param> context = context(true, new TenantBusiness("b1", "t1", 0, null));

        context.initSession(new Param("t1"));
        assertEquals("business b1", context.getFirstMatchedExtension(Pay.class).pay());
    }

    @Test
    public void testOpenScopedSession() throws Exception {
        DefaultExtensionContext<Param> context = context(true, new TenantBusiness("b1", "t1", 0, List.of()));

        try (ExtensionSessionScope ignored = ExtensionSessionScope.openScoped(context, "S", new Param("t1"))) {
            assertEquals("business b1", context.getFirstMatchedExtension("S", Pay.class).pay());
        }
        assertThrows(QueryException.class, () -> context.getFirstMatchedExtension("S", Pay.class));
    }
}
