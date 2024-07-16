package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

import java.util.List;

public class Business {
}

class Business1 extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "Business1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business1");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Ability1", 200));
    }
}

class Business2 extends AbstractBusiness<Object> implements Ext1 {
    @Override
    public String code() {
        return "Business2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business1");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Ability1", 200));
    }
}

@ExtensionPoint
interface Ext1 {

}