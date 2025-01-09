package io.github.xiaoshicae.extension.proxy.extpoint;

import io.github.xiaoshicae.extension.core.AbstractExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;

import java.util.List;

public class Model {
}

class MyParam {
    String name;

    MyParam(String name) {
        this.name = name;
    }
}

interface ExtA {
    String a();
}

interface ExtB {
    String b();
}

class DefaultImpl extends AbstractExtensionPointDefaultImplementation<MyParam> implements ExtA, ExtB {

    @Override
    public String a() {
        return "DefaultImpl a";
    }

    @Override
    public String b() {
        return "DefaultImpl b";
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class);
    }
}

class AbilityA extends AbstractAbility<MyParam> implements ExtA, ExtB {

    @Override
    public String code() {
        return "AbilityA";
    }

    @Override
    public Boolean match(MyParam param) {
        return param.name.contains("a");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class);
    }

    @Override
    public String a() {
        return "AbilityA a";
    }

    @Override
    public String b() {
        return "AbilityA b";
    }
}

class AbilityB extends AbstractAbility<MyParam> implements ExtA, ExtB {

    @Override
    public String code() {
        return "AbilityB";
    }

    @Override
    public Boolean match(MyParam param) {
        return param.name.contains("b");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class);
    }

    @Override
    public String a() {
        return "AbilityB a";
    }

    @Override
    public String b() {
        return "AbilityB b";
    }
}


class BizA extends AbstractBusiness<MyParam> implements ExtA, ExtB {

    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityA", 5), new UsedAbility("AbilityB", 10));
    }

    @Override
    public String code() {
        return "a";
    }

    @Override
    public Boolean match(MyParam param) {
        return param.name.contains("biza");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class);
    }

    @Override
    public String a() {
        return "BizA a";
    }

    @Override
    public String b() {
        return "BizA b";
    }
}


class BizB extends AbstractBusiness<MyParam> implements ExtA, ExtB {

    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityA", -10), new UsedAbility("AbilityB", -5));
    }

    @Override
    public String code() {
        return "b";
    }

    @Override
    public Boolean match(MyParam param) {
        return param.name.contains("bizb");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtB.class);
    }

    @Override
    public String a() {
        return "BizB a";
    }

    @Override
    public String b() {
        return "BizB b";
    }
}

