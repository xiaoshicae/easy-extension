package io.github.xiaoshicae.extension.core.extension;

import java.util.List;

class ExtensionPointGroupImplementation1 extends AbstractExtensionPointGroupImplementation<Object> {

    @Override
    public String code() {
        return "ExtGroupRealization1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization1");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }
}

class ExtensionPointGroupImplementation2 extends AbstractExtensionPointGroupImplementation<Object> implements ExtA {

    @Override
    public String code() {
        return "ExtGroupRealization2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization2");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class);
    }
}

class ExtensionPointGroupImplementation3 extends AbstractExtensionPointGroupImplementation<Object> implements ExtA, ExtB {

    @Override
    public String code() {
        return "ExtGroupRealization1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("ExtGroupRealization1");
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(ExtA.class, ExtB.class);
    }
}

interface ExtA {
}

interface ExtB {
}
