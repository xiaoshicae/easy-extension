package io.github.xiaoshicae.extension.proxy;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Matcher;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.proxy.exception.ProxyException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtensionContextProxyRegisterHelperTest {

    @Test
    public void testRegisterByAnn() throws ProxyException, RegisterException {
        IExtensionContext<MyParam> extensionContext = new DefaultExtensionContext<>();
        ExtensionContextProxyRegisterHelper<MyParam> helper = new ExtensionContextProxyRegisterHelper<>(extensionContext);

        helper.setMatcherParamClass(MyParam.class);
        helper.setExtensionPointDefaultImplementation(new DefaultImpl());
        helper.addExtensionPointClasses(EA.class, EB.class);
        helper.addAbilities(new A1(), new A2(), new A3());
        helper.addBusinesses(new B1(), new B2(), new B3());

        helper.doRegister();

        Class<MyParam> matcherParamClass = extensionContext.getMatcherParamClass();
        assertEquals(MyParam.class, matcherParamClass);

        IExtensionPointGroupDefaultImplementation<MyParam> dImpl = extensionContext.getExtensionPointDefaultImplementation();
        assertEquals(((EA) dImpl).a(), "DefaultImpl a");

        List<Class<?>> classes = extensionContext.listAllExtensionPoint();
        assertArrayEquals(new Class[]{EA.class, EB.class}, classes.toArray());

        List<IAbility<MyParam>> iAbilities = extensionContext.listAllAbility();
        assertEquals("a", iAbilities.get(0).code());
        assertEquals("b", iAbilities.get(1).code());
        assertEquals("a3", iAbilities.get(2).code());
        assertEquals("A1 a", ((EA) iAbilities.get(0)).a());
        assertEquals("A2 a", ((EA) iAbilities.get(1)).a());
        assertEquals("A3 a", ((EA) iAbilities.get(2)).a());

        List<IBusiness<MyParam>> iBusinesses = extensionContext.listAllBusiness();
        assertEquals("b1", iBusinesses.get(0).code());
        assertEquals("b2", iBusinesses.get(1).code());
        assertEquals("b3", iBusinesses.get(2).code());
        assertEquals("B1 a", ((EA) iBusinesses.get(0)).a());
    }
}

@ExtensionPoint
interface EA {
    String a();
}

@ExtensionPoint
interface EB {
}

@MatcherParam
class MyParam {
    String name;
}

@ExtensionPointDefaultImplementation
class DefaultImpl implements EA, EB {
    @Override
    public String a() {
        return "DefaultImpl a";
    }
}

@Ability(code = "a")
class A1 implements Matcher<MyParam>, EA {
    @Override
    public String a() {
        return "A1 a";
    }

    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "a");
    }
}

@Ability(code = "b")
class A2 implements Matcher<MyParam>, EA, EB {
    @Override
    public String a() {
        return "A2 a";
    }

    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "b");
    }
}

class A3 implements IAbility<MyParam>, EA, EB {

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(EB.class);
    }

    @Override
    public String code() {
        return "a3";
    }

    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "c");
    }

    @Override
    public String a() {
        return "A3 a";
    }
}

@Business(code = "b1")
class B1 implements Matcher<MyParam>, EA {
    @Override
    public String a() {
        return "B1 a";
    }

    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "b1");
    }
}

@Business(code = "b2", abilities = {"a", "b"})
class B2 implements Matcher<MyParam> {
    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "b2");
    }
}

class B3 implements IBusiness<MyParam> {
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public String code() {
        return "b3";
    }

    @Override
    public Boolean match(MyParam param) {
        return Objects.equals(param.name, "b3");
    }
}