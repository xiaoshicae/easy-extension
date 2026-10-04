package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The warning about several matching businesses (picked by registration order) is logged once per distinct
 * combination. The combinations were remembered by a hash of their codes, so two different ones with the same hash
 * were taken for one, and the second was never reported.
 */
public class MultiMatchWarningTest {

    @Test
    public void testCombinationsWhoseCodesHashAlikeAreWarnedAboutSeparately() throws Exception {
        // "Aa" and "BB" have the same String.hashCode()
        assertEquals("Aa".hashCode(), "BB".hashCode());

        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.DEFAULT, MultiMatchPolicy.SELECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(new ShopBusiness("Aa", r -> "first".equals(r.name), 0, List.of(Pricing.class), List.of()));
        context.registerBusiness(new ShopBusiness("BB", r -> "second".equals(r.name), 1, List.of(Pricing.class), List.of()));
        context.registerBusiness(new ShopBusiness("anybody", r -> true, 2, List.of(Pricing.class), List.of()));

        context.initSession(new Req("first"));       // matches [Aa, anybody]
        assertEquals(1, context.multiMatchWarningCount());
        context.initSession(new Req("second"));      // matches [BB, anybody]: another combination
        assertEquals(2, context.multiMatchWarningCount(), "a different combination is reported on its own");
        context.removeSession();
    }
}
