package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.ProxyParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.proxy.AbilityProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.BusinessProxyFactory;
import io.github.xiaoshicae.extension.core.proxy.ExtPointDefaultImplProxyFactory;
import io.github.xiaoshicae.extension.core.trace.ExtensionExplanation;
import io.github.xiaoshicae.extension.core.util.AnnProxyConvertUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Registering implementations that a container (Spring AOP, for instance) has wrapped in a proxy.
 * <p>
 * The proxy class carries none of the implementation's metadata, neither its annotations nor its interfaces,
 * so the framework has to read it from the target class everywhere: when registering, when validating
 * {@code @Ability(requires / excludes)}, and when reporting which class implements what.
 * </p>
 */
public class TargetClassRegistrationTest {

    @ExtensionPoint
    public interface Greeting {
        String greet();
    }

    public static class Param {
        final String name;

        public Param(String name) {
            this.name = name;
        }
    }

    @ExtensionPointDefaultImplementation
    public static class DefaultGreeting implements Greeting {
        @Override
        public String greet() {
            return "default";
        }
    }

    @Ability(code = "ability.base")
    public static class BaseAbility implements Matcher<Param>, Greeting {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "base ability";
        }
    }

    @Ability(code = "ability.vip", requires = {"ability.base"})
    public static class VipAbility implements Matcher<Param>, Greeting {
        @Override
        public boolean match(Param param) {
            return true;
        }

        @Override
        public String greet() {
            return "vip ability";
        }
    }

    @Business(code = "biz.shop", abilities = {"ability.vip"})
    public static class ShopBusiness implements Matcher<Param>, Greeting {
        @Override
        public boolean match(Param param) {
            return "shop".equals(param.name);
        }

        @Override
        public String greet() {
            return "shop business";
        }
    }

    @Business(code = "biz.base", abilities = {"ability.base"})
    public static class BaseBusiness implements Matcher<Param>, Greeting {
        @Override
        public boolean match(Param param) {
            return "base".equals(param.name);
        }

        @Override
        public String greet() {
            return "base business";
        }
    }

    /** What Spring AOP hands out for a class with an interface: implements its interfaces, carries nothing else. */
    @SuppressWarnings("unchecked")
    private static Matcher<Param> containerProxyOf(Object target) {
        return (Matcher<Param>) Proxy.newProxyInstance(
                TargetClassRegistrationTest.class.getClassLoader(),
                new Class<?>[]{Matcher.class, Greeting.class},
                (proxy, method, args) -> method.invoke(target, args));
    }

    private static DefaultExtensionContext<Param> newContext() throws Exception {
        DefaultExtensionContext<Param> context = new DefaultExtensionContext<>();
        context.registerExtensionPoint(Greeting.class);
        context.registerMatcherParamClass(Param.class);
        context.registerExtensionPointDefaultImplementation(
                AnnProxyConvertUtils.convertAnnExtensionPointGroupDefaultImplementation(new DefaultGreeting()));
        return context;
    }

    @Test
    public void testAbilityConstraintsAreEnforcedForProxiedAbilities() throws Exception {
        DefaultExtensionContext<Param> context = newContext();
        context.registerAbility(AnnProxyConvertUtils.convertAnnAbilityToProxy(containerProxyOf(new VipAbility()), VipAbility.class));

        // ability.vip requires ability.base, which this business does not mount
        RegisterException e = assertThrows(RegisterException.class, () -> context.registerBusiness(
                AnnProxyConvertUtils.convertAnnBusinessToProxy(containerProxyOf(new ShopBusiness()), ShopBusiness.class)));
        assertEquals("business [biz.shop] mounts ability [ability.vip] which requires ability [ability.base], but [ability.base] is not mounted",
                e.getMessage());
    }

    @Test
    public void testConstructorsWithoutTargetClassStillReportANullInstanceAsAProxyException() {
        String expected = "The instance does not implement the extension point: " + Greeting.class.getName();

        ProxyException e = assertThrows(ProxyParamException.class,
                () -> new AbilityProxyFactory<Param>("ability.base", null, List.of(Greeting.class)));
        assertEquals(expected, e.getMessage());

        e = assertThrows(ProxyParamException.class,
                () -> new BusinessProxyFactory<Param>("biz.base", 0, List.of(), null, List.of(Greeting.class)));
        assertEquals(expected, e.getMessage());

        e = assertThrows(ProxyParamException.class,
                () -> new ExtPointDefaultImplProxyFactory<Param>(null, List.of(Greeting.class)));
        assertEquals(expected, e.getMessage());
    }

    @Test
    public void testExplainNamesTheTargetClassesNotTheProxies() throws Exception {
        DefaultExtensionContext<Param> context = newContext();
        context.registerAbility(AnnProxyConvertUtils.convertAnnAbilityToProxy(containerProxyOf(new BaseAbility()), BaseAbility.class));
        context.registerBusiness(AnnProxyConvertUtils.convertAnnBusinessToProxy(containerProxyOf(new BaseBusiness()), BaseBusiness.class));

        context.initSession(new Param("base"));
        try {
            assertEquals("base business", context.invoke(Greeting.class, Greeting::greet));

            ExtensionExplanation<Greeting> explanation = context.explain(Greeting.class);
            List<Class<?>> implementations = explanation.candidates().stream()
                    .map(ExtensionExplanation.Candidate::implementationClass).toList();
            assertEquals(List.of(BaseBusiness.class, BaseAbility.class, DefaultGreeting.class), implementations);
            assertEquals(BaseBusiness.class, explanation.selected().implementationClass());
        } finally {
            context.removeSession();
        }
    }
}
