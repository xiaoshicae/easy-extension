package io.github.xiaoshicae.extension.core.ability;


import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

public class Ability {
}

class Ability1 extends AbstractAbility<Object> implements Ext1 {
    @Override
    public String code() {
        return "Ability1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Ability1");
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
}


interface Ext1 {
}

@ExtensionPoint
interface Ext2 {
}
