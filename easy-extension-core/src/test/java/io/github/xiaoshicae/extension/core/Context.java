package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;

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
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200));
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
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 10));
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
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return null;
    }
}


class BusinessUnknownAbilityCode extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessUnknownAbilityCode";
    }

    @Override
    public Boolean match(Object param) {
        return false;
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Unknown", 200));
    }
}


class BusinessUsedAbilityCodeDuplicate extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessUsedAbilityCodeDuplicate";
    }

    @Override
    public Boolean match(Object param) {
        return false;
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200), new UsedAbility("AbilityM", 300));
    }
}


class BusinessUsedAbilityPriorityDuplicate extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessUsedAbilityPriorityDuplicate";
    }

    @Override
    public Boolean match(Object param) {
        return false;
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200), new UsedAbility("AbilityN", 200));
    }
}


class BusinessUsedAbilityPriority extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessUsedAbilityPriority";
    }

    @Override
    public Boolean match(Object param) {
        return false;
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200), new UsedAbility("AbilityN", 300));
    }
}


class BusinessPriorityConflict extends AbstractBusiness<Object> implements ExtA {
    @Override
    public String code() {
        return "BusinessPriorityConflict";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessPriorityConflict");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 100));
    }

    @Override
    public String extA() {
        return "BusinessPriorityConflict extA";
    }
}

class AbilityL extends AbstractAbility<Object> {
    @Override
    public String code() {
        return "AbilityL";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityL");
    }
}

class AbilityM extends AbstractAbility<Object> implements ExtA, ExtB {
    @Override
    public String code() {
        return "AbilityM";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityM") || param.toString().contains("BusinessPriorityConflict");
    }


    @Override
    public String extA() {
        return "AbilityM exec extA";
    }

    @Override
    public String extB() {
        return "AbilityM exec extB";
    }
}

class AbilityN extends AbstractAbility<Object> implements ExtB, ExtC {
    @Override
    public String code() {
        return "AbilityN";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityN");
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

@ExtensionPoint
interface ExtA {
    String extA();
}

@ExtensionPoint
interface ExtB {
    String extB();
}

@ExtensionPoint
interface ExtC {
    String extC();
}

class ExtDefaultAbility extends BaseDefaultAbility<Object> implements ExtA, ExtB, ExtC {
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
}


class ExtDefaultAbilityInvalid extends BaseDefaultAbility<Object> implements ExtB, ExtC {
    @Override
    public String extB() {
        return "ExtDefaultAbility do extB";
    }

    @Override
    public String extC() {
        return "ExtDefaultAbility do extC";
    }
}
