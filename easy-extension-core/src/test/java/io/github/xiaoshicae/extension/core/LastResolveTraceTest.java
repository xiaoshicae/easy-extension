package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code getLastResolveTrace()} is the debugging tool for "why did this request go to that business". It has to
 * agree with what routing does: a request whose {@code initSession} failed has no session, so it has no trace either
 * (it used to report the request that was resolved before it on the same thread).
 */
public class LastResolveTraceTest {

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(new ShopBusiness("retail", r -> "retail".equals(r.name), 0, List.of(Pricing.class), List.of()));
        return context;
    }

    @Test
    public void testFailedInitSessionDoesNotReportTheTraceOfAnEarlierRequest() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.initSession(new Req("retail"));
        try {
            assertEquals("retail", context.getLastResolveTrace().getMatchedBusinessCode());

            SessionException e = assertThrows(SessionException.class, () -> context.initSession(new Req("nobody")));
            assertEquals("no business matched", e.getMessage());

            // routing says there is no session ...
            QueryException noSession = assertThrows(QueryException.class, () -> context.getFirstMatchedExtension(Pricing.class));
            assertEquals("scope [__easy__extension__default__scope__], matched codes is empty, may be session not init", noSession.getMessage());
            // ... so the trace must not claim the earlier request is still the current one
            assertNull(context.getLastResolveTrace());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testFailedInitOfAnotherScopeKeepsTheTraceOfTheScopeThatIsStillBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        context.initSession(new Req("retail"));
        try {
            assertThrows(SessionException.class, () -> context.initSession("tenant-x", new Req("nobody")));

            // the default scope was not touched
            assertEquals("retail", context.getLastResolveTrace().getMatchedBusinessCode());
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        } finally {
            context.removeSession();
        }
    }
}
