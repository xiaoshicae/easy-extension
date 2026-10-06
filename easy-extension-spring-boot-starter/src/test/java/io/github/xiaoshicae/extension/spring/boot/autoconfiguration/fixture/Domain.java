package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small domain that the starter tests scan: extension points, their default implementations, abilities and businesses,
 * some of which use {@code @ExtensionInject} themselves.
 */
public final class Domain {
    private Domain() {
    }

    public record Param(String tenant) {
    }

    @ExtensionPoint
    public interface Pay {
        String pay();
    }

    @ExtensionPoint
    public interface Ship {
        String ship();
    }

    @DefaultImplementation
    public static class DefaultPay implements Pay {
        @Override
        public String pay() {
            return "default-pay";
        }
    }

    @DefaultImplementation
    public static class DefaultShip implements Ship {
        @Override
        public String ship() {
            return "default-ship";
        }
    }

    /** Counts its instances: a scanned provider is one Spring bean, created once. */
    @Ability(code = "ability.fast-ship")
    public static class FastShipAbility implements Matcher<Param>, Ship {
        public static final AtomicInteger INSTANCES = new AtomicInteger();

        public FastShipAbility() {
            INSTANCES.incrementAndGet();
        }

        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String ship() {
            return "fast-ship";
        }
    }

    @Business(code = "biz.retail", abilities = FastShipAbility.class)
    public static class RetailBusiness implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return "retail".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "retail-pay";
        }
    }

    /** A plain bean that uses extension points. */
    public static class OrderService {
        @ExtensionInject
        public Ship ship;

        @ExtensionInject
        public List<Pay> allPay;
    }

    /**
     * An ability that uses another extension point itself, directly and through a bean that does: with the 3.x wiring
     * the application could not even start.
     */
    @Ability(code = "ability.composed")
    public static class ComposedAbility implements Matcher<Param>, Pay {
        @ExtensionInject
        private Ship ship;

        @Autowired
        private OrderService orderService;

        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String pay() {
            return "composed(" + ship.ship() + "," + orderService.ship.ship() + ")";
        }
    }

    @Business(code = "biz.composed", abilities = ComposedAbility.class)
    public static class ComposedBusiness implements Matcher<Param>, Ship {
        @Override
        public boolean match(Param param) {
            return "composed".equals(param.tenant());
        }

        @Override
        public String ship() {
            return "composed-ship";
        }
    }

    /** An {@code @ExtensionPoint} on a class is not an extension point: ignored by the scan. */
    @ExtensionPoint
    public static class NotAnExtensionPoint {
    }
}
