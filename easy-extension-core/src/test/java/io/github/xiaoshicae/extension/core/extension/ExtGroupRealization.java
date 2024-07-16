package io.github.xiaoshicae.extension.core.extension;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;

public class ExtGroupRealization {
}

class ExtGroupRealization1 extends AbstractExtGroupRealization<Object> {
    @Override
    public String code() {
        return "ExtGroupRealization1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization1");
    }
}




class ExtGroupRealization2 extends AbstractExtGroupRealization<Object> implements ExtA {
    @Override
    public String code() {
        return "ExtGroupRealization2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization2");
    }
}

class ExtGroupRealization3 extends AbstractExtGroupRealization<Object> implements ExtA, ExtB {
    @Override
    public String code() {
        return "ExtGroupRealization1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization1");
    }
}

@ExtensionPoint
interface ExtA {
}

@ExtensionPoint
interface ExtB {
}