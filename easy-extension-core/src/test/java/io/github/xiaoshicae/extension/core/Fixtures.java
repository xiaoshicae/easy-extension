package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.DefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.Self;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.Set;

/**
 * A small payment/shipping domain shared by the tests.
 */
public final class Fixtures {
    private Fixtures() {
    }

    public record Param(String tenant, Set<String> flags) {
        public static Param of(String tenant, String... flags) {
            return new Param(tenant, Set.of(flags));
        }

        boolean has(String flag) {
            return flags.contains(flag);
        }
    }

    @ExtensionPoint
    public interface Pay {
        String pay();
    }

    @ExtensionPoint
    public interface Ship {
        String ship();
    }

    @ExtensionPoint(optional = true)
    public interface Audit {
        String audit();
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

    @Ability(code = "ability.alipay")
    public static class AlipayAbility implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return param.has("alipay");
        }

        @Override
        public String pay() {
            return "ability-alipay";
        }
    }

    @Ability(code = "ability.fast-ship")
    public static class FastShipAbility implements Matcher<Param>, Ship {
        @Override
        public boolean match(Param param) {
            return param.has("fast");
        }

        @Override
        public String ship() {
            return "ability-fast-ship";
        }
    }

    @Ability(code = "ability.risk")
    public static class RiskAbility implements Matcher<Param>, Audit {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String audit() {
            return "ability-risk";
        }
    }

    @Ability(code = "ability.installment", requires = RiskAbility.class)
    public static class InstallmentAbility implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return param.has("installment");
        }

        @Override
        public String pay() {
            return "ability-installment";
        }
    }

    @Ability(code = "ability.slow-ship", excludes = FastShipAbility.class)
    public static class SlowShipAbility implements Matcher<Param>, Ship {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String ship() {
            return "ability-slow-ship";
        }
    }

    /** Business first (the default), then its abilities. */
    @Business(code = "biz.retail", uses = {AlipayAbility.class, FastShipAbility.class})
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

    @Business(code = "biz.retail.2", uses = AlipayAbility.class)
    public static class SecondRetailBusiness implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return "retail".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "retail2-pay";
        }
    }

    /** The ability overrides the business: the business places itself after it. */
    @Business(code = "biz.fresh", uses = {AlipayAbility.class, Self.class})
    public static class FreshBusiness implements Matcher<Param>, Pay, Ship {
        @Override
        public boolean match(Param param) {
            return "fresh".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "fresh-pay";
        }

        @Override
        public String ship() {
            return "fresh-ship";
        }
    }

    @Business(code = "biz.audited", uses = RiskAbility.class)
    public static class AuditedBusiness implements Matcher<Param>, Pay {
        @Override
        public boolean match(Param param) {
            return "audited".equals(param.tenant());
        }

        @Override
        public String pay() {
            return "audited-pay";
        }
    }

    /** A builder with the extension points, their default implementations and a few abilities registered. */
    public static ExtensionContextBuilder<Param> base() {
        return ExtensionContext.<Param>builder()
                .extensionPoint(Pay.class, Ship.class, Audit.class)
                .defaultImplementation(new DefaultPay())
                .defaultImplementation(new DefaultShip())
                .ability(new AlipayAbility())
                .ability(new FastShipAbility())
                .ability(new RiskAbility());
    }
}
