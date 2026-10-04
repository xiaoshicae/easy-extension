package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The proxy of a business, an ability or a default implementation answers the methods of {@code IBusiness} /
 * {@code IAbility} / {@code IProxy} itself and forwards everything else to the implementation. An extension point
 * that declares one of those methods ({@code code()} is not an unlikely name) is therefore answered by the framework,
 * and the implementation is never asked. The README warns about {@code getInstance()} and {@code getTargetClass()}
 * only; the others used to go unnoticed.
 */
public class ExtensionPointMethodClashTest {

    @ExtensionPoint
    public interface PaymentChannel {
        /** The code of the channel, as the payment provider knows it. */
        String code();
    }

    @ExtensionPointDefaultImplementation
    public static class DefaultChannel implements PaymentChannel {
        @Override
        public String code() {
            return "default channel";
        }
    }

    @Ability(code = "ability.alipay")
    public static class AlipayAbility implements Matcher<Req>, PaymentChannel {
        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public String code() {
            return "ALIPAY";
        }
    }

    @Business(code = "biz.shop")
    public static class ShopBusiness implements Matcher<Req>, PaymentChannel {
        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public String code() {
            return "WECHAT";
        }
    }

    @Test
    public void testBusinessWhoseExtensionPointClashesWithTheFrameworkIsRefused() {
        ProxyParamException e = assertThrows(ProxyParamException.class,
                () -> AnnProxyConvertUtils.convertAnnBusinessToProxy(new ShopBusiness()));
        assertEquals("extension point [" + PaymentChannel.class.getName() + "] declares method code(), which the framework answers itself for a business; rename the method",
                e.getMessage());
    }

    @Test
    public void testAbilityWhoseExtensionPointClashesWithTheFrameworkIsRefused() {
        ProxyParamException e = assertThrows(ProxyParamException.class,
                () -> AnnProxyConvertUtils.convertAnnAbilityToProxy(new AlipayAbility()));
        assertEquals("extension point [" + PaymentChannel.class.getName() + "] declares method code(), which the framework answers itself for an ability; rename the method",
                e.getMessage());
    }

    @Test
    public void testDefaultImplementationWhoseExtensionPointClashesWithTheFrameworkIsRefused() {
        ProxyParamException e = assertThrows(ProxyParamException.class,
                () -> AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new DefaultChannel()));
        assertEquals("extension point [" + PaymentChannel.class.getName() + "] declares method code(), which the framework answers itself for a default implementation; rename the method",
                e.getMessage());
    }
}
