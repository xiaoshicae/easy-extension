package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;

import java.util.List;

public class Context {
}


class BusinessX extends AbstractBusiness<Object> implements ExtA {
    @Override
    public String code() {
        return "BusinessX";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessX");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtD.class);
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200));
    }


    @Override
    public String extA() {
        return "BusinessX exec extA";
    }
}

class BusinessY extends AbstractBusiness<Object> implements ExtB {
    @Override
    public String code() {
        return "BusinessY";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessY");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtB.class);
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Unknown", 200));
    }


    @Override
    public String extB() {
        return "BusinessY exec extB";
    }
}

class BusinessZ extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessZ";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessZ");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityN", 10), new UsedAbility("AbilityN", 20));
    }
}

class BusinessZZ extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessZZ";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessZZ");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityN", 10), new UsedAbility("AbilityNN", 10));
    }
}


class BusinessZZZ extends AbstractBusiness<Object> implements ExtA, ExtB {
    @Override
    public String code() {
        return "BusinessZZZ";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("XXX");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class);
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityN", 10), new UsedAbility("AbilityNN", 20));
    }

    @Override
    public String extA() {
        return "BusinessZZZ extA";
    }

    @Override
    public String extB() {
        return "BusinessZZZ extB";
    }
}


class BusinessC1 extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessC1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessC");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityN", 10), new UsedAbility("AbilityNN", 20));
    }
}


class BusinessC2 extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessC2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessC");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityN", 10), new UsedAbility("AbilityNN", 20));
    }
}

class AbilityL extends AbstractAbility<Object> implements ExtD {
    @Override
    public String code() {
        return "AbilityL";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityL");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtD.class);
    }

    @Override
    public String extC() {
        return "";
    }
}

class AbilityM extends AbstractAbility<Object> {
    @Override
    public String code() {
        return "AbilityM";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityM") || param.toString().contains("BusinessPriorityConflict");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }
}

class AbilityN extends AbstractAbility<Object> implements ExtB, ExtC {
    @Override
    public String code() {
        return "AbilityN";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("XXX");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtB.class, ExtC.class);
    }

    @Override
    public String extB() {
        return "AbilityN exec extB";
    }

    @Override
    public String extC() {
        return "AbilityN exec extC";
    }
}


class AbilityNN extends AbstractAbility<Object> implements ExtB, ExtC {
    @Override
    public String code() {
        return "AbilityNN";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("XXX");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtB.class, ExtC.class);
    }

    @Override
    public String extB() {
        return "AbilityNN exec extB";
    }

    @Override
    public String extC() {
        return "AbilityNN exec extC";
    }
}


interface ExtA {
    String extA();
}

interface ExtB {
    String extB();
}

interface ExtC {
    String extC();
}

interface ExtD {
    String extC();
}

class ExtensionPointDefaultImplementation extends AbstractExtensionPointDefaultImplementation<Object> implements ExtA, ExtB, ExtC {
    @Override
    public String extA() {
        return "ExtDefaultAbility do extA";
    }

    @Override
    public String extB() {
        return "ExtDefaultAbility do extB";
    }

    @Override
    public String extC() {
        return "ExtDefaultAbility do extC";
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class, ExtC.class);
    }
}
