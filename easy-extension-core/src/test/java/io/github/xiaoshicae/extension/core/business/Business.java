package io.github.xiaoshicae.extension.core.business;

import java.util.List;

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
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
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
    public List<Class<?>> implementExtensionPoints() {
        return List.of(String.class);
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


class Business3 extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "Business3";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business3");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class);
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


class Business4 extends AbstractBusiness<Object> implements Ext1 {
    @Override
    public String code() {
        return "Business4";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business1");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class);
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

interface Ext1 {
}