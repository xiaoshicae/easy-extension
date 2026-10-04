package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.AllDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Looking an extension up in a named scope fails the way it does in the default scope: the same exception type
 * ({@code IExtensionFactory} documents {@code QueryNotFoundException} for both) and the same reason (here: the
 * extension point is mandatory and nobody in the chain implements it). The scoped lookup wrapped the failure in a
 * plain {@code QueryException} whose message only said "failed", and {@code invoke} copied that message.
 */
public class ScopedLookupFailureTest {

    private static final String DEFAULT_CODE = "system.extension.point.default.implementation";

    /** Pricing and Shipping have a default, the mandatory Tax is implemented by nobody. */
    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.addExtensionPointDefaultImplementation(new AllDefaults());
        context.registerBusiness(new ShopBusiness("a", r -> true, 0, List.of(Pricing.class), List.of()));
        context.initSession(new Req("x"));
        context.initSession("s", new Req("x"));
        return context;
    }

    private static final String REASON = "Extension<" + Tax.class.getName() + "> not found, it is a mandatory extension point and none of [a > "
            + DEFAULT_CODE + "] implements it";

    @Test
    public void testScopedLookupThrowsNotFoundWithTheReason() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        try {
            QueryNotFoundException inDefaultScope = assertThrows(QueryNotFoundException.class, () -> context.getFirstMatchedExtension(Tax.class));
            assertEquals(REASON, inDefaultScope.getMessage());

            QueryNotFoundException inScope = assertThrows(QueryNotFoundException.class, () -> context.getFirstMatchedExtension("s", Tax.class));
            assertEquals("get first matched Extension<Tax> with scope: [s] failed, " + REASON, inScope.getMessage());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testScopedInvokeKeepsTheReason() throws Exception {
        DefaultExtensionContext<Req> context = shop();
        try {
            InvokeException inDefaultScope = assertThrows(InvokeException.class, () -> context.invoke(Tax.class, Tax::tax));
            assertEquals("invoke failed, " + REASON, inDefaultScope.getMessage());

            InvokeException inScope = assertThrows(InvokeException.class, () -> context.invoke("s", Tax.class, Tax::tax));
            assertEquals("scope s invoke failed, get first matched Extension<Tax> with scope: [s] failed, " + REASON, inScope.getMessage());
        } finally {
            context.removeSession();
        }
    }
}
