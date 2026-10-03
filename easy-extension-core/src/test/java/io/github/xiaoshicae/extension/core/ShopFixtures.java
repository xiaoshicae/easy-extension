package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;

import java.util.List;
import java.util.function.Predicate;

/**
 * A small shop domain shared by the context-level tests: three extension points (one of them mandatory),
 * configurable businesses and abilities, and default implementations that can be split up in different ways.
 */
public final class ShopFixtures {

    private ShopFixtures() {
    }

    @ExtensionPoint
    public interface Pricing {
        String price();

        String discounted(int percent);
    }

    @ExtensionPoint
    public interface Shipping {
        String ship();
    }

    /** No sensible default: whoever serves the request has to bring it. */
    @ExtensionPoint(mandatory = true)
    public interface Tax {
        String tax();
    }

    /** The request, as far as matching is concerned. */
    public static final class Req {
        public final String name;

        public Req(String name) {
            this.name = name;
        }
    }

    /** Default implementation for {@link Pricing} only. */
    public static final class PricingDefault extends AbstractExtensionPointDefaultImplementation<Req> implements Pricing {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class);
        }

        @Override
        public String price() {
            return "default pricing";
        }

        @Override
        public String discounted(int percent) {
            return "default pricing -" + percent + "%";
        }
    }

    /** Default implementation for {@link Shipping} only. */
    public static final class ShippingDefault extends AbstractExtensionPointDefaultImplementation<Req> implements Shipping {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Shipping.class);
        }

        @Override
        public String ship() {
            return "default shipping";
        }
    }

    /** Default implementation for everything that has a default: {@link Pricing} and {@link Shipping}. */
    public static final class AllDefaults extends AbstractExtensionPointDefaultImplementation<Req> implements Pricing, Shipping {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class, Shipping.class);
        }

        @Override
        public String price() {
            return "default pricing";
        }

        @Override
        public String discounted(int percent) {
            return "default pricing -" + percent + "%";
        }

        @Override
        public String ship() {
            return "default shipping";
        }
    }

    /** Default implementation for all three extension points, the mandatory {@link Tax} included. */
    public static final class FullDefaults extends AbstractExtensionPointDefaultImplementation<Req> implements Pricing, Shipping, Tax {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class, Shipping.class, Tax.class);
        }

        @Override
        public String price() {
            return "default pricing";
        }

        @Override
        public String discounted(int percent) {
            return "default pricing -" + percent + "%";
        }

        @Override
        public String ship() {
            return "default shipping";
        }

        @Override
        public String tax() {
            return "default tax";
        }
    }

    /** Default implementation that implements nothing. */
    public static final class EmptyDefault extends AbstractExtensionPointDefaultImplementation<Req> {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of();
        }
    }

    /** A default implementation with a code and a priority of its own, as the interface allows. */
    public static final class CustomDefault implements IExtensionPointGroupDefaultImplementation<Req>, Shipping {
        private final String code;
        private final Integer priority;

        public CustomDefault(String code, Integer priority) {
            this.code = code;
            this.priority = priority;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public Integer priority() {
            return priority;
        }

        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Shipping.class);
        }

        @Override
        public String ship() {
            return code + " shipping";
        }
    }

    /** Like {@link CustomDefault}, for {@link Pricing}: a code and a priority of its own. */
    public static final class CustomPricingDefault implements IExtensionPointGroupDefaultImplementation<Req>, Pricing {
        private final String code;
        private final Integer priority;

        public CustomPricingDefault(String code, Integer priority) {
            this.code = code;
            this.priority = priority;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public Integer priority() {
            return priority;
        }

        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Pricing.class);
        }

        @Override
        public String price() {
            return code + " pricing";
        }

        @Override
        public String discounted(int percent) {
            return code + " pricing -" + percent + "%";
        }
    }

    /**
     * A business that implements whichever of the three extension points it is told to, and answers with its code.
     */
    public static class ShopBusiness extends AbstractBusiness<Req> implements Pricing, Shipping, Tax {
        private final String code;
        private final Predicate<Req> matcher;
        private final int priority;
        private final List<Class<?>> extensionPoints;
        private final List<UsedAbility> usedAbilities;

        public ShopBusiness(String code, Predicate<Req> matcher, int priority, List<Class<?>> extensionPoints, List<UsedAbility> usedAbilities) {
            this.code = code;
            this.matcher = matcher;
            this.priority = priority;
            this.extensionPoints = extensionPoints;
            this.usedAbilities = usedAbilities;
        }

        /** Matches the request with this name, has priority 0, mounts no ability. */
        public static ShopBusiness named(String code, Class<?>... extensionPoints) {
            return new ShopBusiness(code, req -> code.equals(req.name), 0, List.of(extensionPoints), List.of());
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public boolean match(Req param) {
            return matcher.test(param);
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
            return extensionPoints;
        }

        @Override
        public String price() {
            return code + " pricing";
        }

        @Override
        public String discounted(int percent) {
            return code + " pricing -" + percent + "%";
        }

        @Override
        public String ship() {
            return code + " shipping";
        }

        @Override
        public String tax() {
            return code + " tax";
        }
    }

    /** An ability that implements whichever of the three extension points it is told to. */
    public static final class ShopAbility extends AbstractAbility<Req> implements Pricing, Shipping, Tax {
        private final String code;
        private final Predicate<Req> matcher;
        private final List<Class<?>> extensionPoints;

        public ShopAbility(String code, Predicate<Req> matcher, Class<?>... extensionPoints) {
            this.code = code;
            this.matcher = matcher;
            this.extensionPoints = List.of(extensionPoints);
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public boolean match(Req param) {
            return matcher.test(param);
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return extensionPoints;
        }

        @Override
        public String price() {
            return code + " pricing";
        }

        @Override
        public String discounted(int percent) {
            return code + " pricing -" + percent + "%";
        }

        @Override
        public String ship() {
            return code + " shipping";
        }

        @Override
        public String tax() {
            return code + " tax";
        }
    }

    /** A context with the three extension points and the matcher param registered. */
    public static DefaultExtensionContext<Req> emptyShop(DefaultExtensionContext<Req> context) throws Exception {
        context.registerExtensionPoint(Pricing.class);
        context.registerExtensionPoint(Shipping.class);
        context.registerExtensionPoint(Tax.class);
        context.registerMatcherParamClass(Req.class);
        return context;
    }
}
