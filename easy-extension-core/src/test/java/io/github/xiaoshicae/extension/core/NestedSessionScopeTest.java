package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.FullDefaults;
import io.github.xiaoshicae.extension.core.ShopFixtures.Pricing;
import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.ShopFixtures.ShopBusiness;
import io.github.xiaoshicae.extension.core.ShopFixtures.Tax;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.exception.InvokeException;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.session.ExtensionSessionScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ExtensionSessionScope#run}, {@link ExtensionSessionScope#open} and {@link ExtensionSessionScope#openScoped}
 * nest: whatever session the thread had before is still there once the inner scope is closed. (Service A, wrapped by an
 * aspect that opens a scope, calls service B, wrapped by the same aspect.)
 */
public class NestedSessionScopeTest {

    private static DefaultExtensionContext<Req> shop() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        ShopFixtures.emptyShop(context);
        context.registerExtensionPointDefaultImplementation(new FullDefaults());
        context.registerBusiness(ShopBusiness.named("retail", Pricing.class, Tax.class));
        context.registerBusiness(ShopBusiness.named("wholesale", Pricing.class, Tax.class));
        return context;
    }

    @Test
    public void testInnerRunLeavesTheOuterSessionBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        try (var outer = ExtensionSessionScope.open(context, new Req("retail"))) {
            assertNotNull(outer);
            String inner = ExtensionSessionScope.run(context, new Req("wholesale"), () -> context.invoke(Pricing.class, Pricing::price));
            assertEquals("wholesale pricing", inner);

            // the outer request carries on after the inner scope is closed
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        }
        assertNull(context.currentChain(), "and the outermost scope leaves nothing behind");
    }

    @Test
    public void testInnerRunWithoutAResultLeavesTheOuterSessionBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        ExtensionSessionScope.run(context, new Req("retail"), () -> {
            try {
                ExtensionSessionScope.run(context, new Req("wholesale"), () -> assertEquals("wholesale pricing", context.invoke(Pricing.class, Pricing::price)));
            } catch (SessionException e) {
                throw new IllegalStateException(e);
            }
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        });

        assertNull(context.currentChain());
    }

    @Test
    public void testInnerRunThatFailsLeavesTheOuterSessionBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        try (var outer = ExtensionSessionScope.open(context, new Req("retail"))) {
            assertNotNull(outer);
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> ExtensionSessionScope.run(context, new Req("wholesale"), () -> {
                throw new IllegalStateException("the inner body fails");
            }));
            assertEquals("the inner body fails", failure.getMessage());

            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        }
    }

    @Test
    public void testInnerOpenLeavesTheOuterSessionBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        try (var outer = ExtensionSessionScope.open(context, new Req("retail"))) {
            assertNotNull(outer);
            try (var inner = ExtensionSessionScope.open(context, new Req("wholesale"))) {
                assertNotNull(inner);
                assertEquals("wholesale pricing", context.invoke(Pricing.class, Pricing::price));
            }
            assertEquals("retail pricing", context.invoke(Pricing.class, Pricing::price));
        }
    }

    @Test
    public void testInnerOpenScopedLeavesTheOuterSessionOfThatScopeBound() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        try (var outer = ExtensionSessionScope.openScoped(context, "tenant-x", new Req("retail"))) {
            assertNotNull(outer);
            try (var inner = ExtensionSessionScope.openScoped(context, "tenant-x", new Req("wholesale"))) {
                assertNotNull(inner);
                assertEquals("wholesale pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
            }
            assertEquals("retail pricing", context.invoke("tenant-x", Pricing.class, Pricing::price));
        }
        assertNull(context.currentChain("tenant-x"));
    }

    @Test
    public void testScopeThatCannotBeInitializedLeavesNoSessionForThatScope() throws Exception {
        DefaultExtensionContext<Req> context = shop();

        try (var outer = ExtensionSessionScope.open(context, new Req("retail"))) {
            assertNotNull(outer);
            assertThrows(SessionException.class, () -> ExtensionSessionScope.run(context, new Req("nobody"), () -> "unreachable"));

            // the inner scope asked for another identity and did not get one: the old one does not serve it
            InvokeException e = assertThrows(InvokeException.class, () -> context.invoke(Pricing.class, Pricing::price));
            assertEquals("invoke failed, scope [__easy__extension__default__scope__], matched codes is empty, may be session not init", e.getMessage());
        }
    }
}
