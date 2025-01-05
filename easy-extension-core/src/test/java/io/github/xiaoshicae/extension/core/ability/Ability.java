package io.github.xiaoshicae.extension.core.ability;


import java.util.List;

class Ability1 extends AbstractAbility<Object> implements Ext1 {
    @Override
    public String code() {
        return "Ability1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability1");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }
}

class Ability2 extends AbstractAbility<Object> implements Ext1, Ext2 {
    @Override
    public String code() {
        return "Ability2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability2");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(String.class);
    }
}


class Ability3 extends AbstractAbility<Object> implements Ext1 {
    @Override
    public String code() {
        return "Ability3";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability3");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext2.class);
    }
}


class Ability4 extends AbstractAbility<Object> implements Ext1, Ext2 {
    @Override
    public String code() {
        return "Ability4";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability3");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class);
    }
}


class Ability5 extends AbstractAbility<Object> implements Ext1, Ext2 {
    @Override
    public String code() {
        return "Ability5";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability3");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(Ext1.class, Ext2.class);
    }
}



interface Ext1 {
}

interface Ext2 {
}
