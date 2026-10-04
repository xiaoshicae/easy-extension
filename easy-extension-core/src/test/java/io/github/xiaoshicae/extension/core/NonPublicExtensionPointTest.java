package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ShopFixtures.Req;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Registering an extension point only asks for an interface, and calling a package-private one through the context
 * works. The framework's proxies call the implementation reflectively, which a package-private interface does not
 * allow: the injected proxy, and every lookup once an interceptor is registered, failed with an
 * {@code UndeclaredThrowableException} whose cause was an {@code IllegalAccessException}.
 */
public class NonPublicExtensionPointTest {

    /** Not public. */
    interface Greeting {
        String hello();
    }

    static final class GreetingBusiness extends AbstractBusiness<Req> implements Greeting {
        @Override
        public String code() {
            return "greeter";
        }

        @Override
        public boolean match(Req param) {
            return true;
        }

        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Greeting.class);
        }

        @Override
        public Integer priority() {
            return 0;
        }

        @Override
        public List<UsedAbility> usedAbilities() {
            return List.of();
        }

        @Override
        public String hello() {
            return "hello from the business";
        }
    }

    static final class GreetingDefault extends AbstractExtensionPointDefaultImplementation<Req> implements Greeting {
        @Override
        public List<Class<?>> implementExtensionPoints() {
            return List.of(Greeting.class);
        }

        @Override
        public String hello() {
            return "hello from the default";
        }
    }

    private static DefaultExtensionContext<Req> context() throws Exception {
        DefaultExtensionContext<Req> context = new DefaultExtensionContext<>(false, UnknownBusinessPolicy.REJECT, MultiMatchPolicy.REJECT, List.of());
        context.registerExtensionPoint(Greeting.class);
        context.registerMatcherParamClass(Req.class);
        context.registerExtensionPointDefaultImplementation(new GreetingDefault());
        context.registerBusiness(new GreetingBusiness());
        return context;
    }

    @Test
    public void testInjectedProxyCallsAPackagePrivateExtensionPoint() throws Exception {
        DefaultExtensionContext<Req> context = context();
        Greeting injected = new FirstMatchedExtPointProxyFactory<>(Greeting.class, context).getProxy();
        context.initSession(new Req("anybody"));
        try {
            assertEquals("hello from the business", injected.hello());
        } finally {
            context.removeSession();
        }
    }

    @Test
    public void testInterceptorDoesNotBreakAPackagePrivateExtensionPoint() throws Exception {
        DefaultExtensionContext<Req> context = context();
        context.initSession(new Req("anybody"));
        try {
            assertEquals("hello from the business", context.invoke(Greeting.class, Greeting::hello), "works without an interceptor");

            context.registerInterceptor(invocation -> invocation.proceed());

            assertEquals("hello from the business", context.invoke(Greeting.class, Greeting::hello), "and with one");
        } finally {
            context.removeSession();
        }
    }
}
