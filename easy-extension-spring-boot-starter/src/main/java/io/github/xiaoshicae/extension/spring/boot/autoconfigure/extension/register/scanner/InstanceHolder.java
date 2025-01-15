package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

public class InstanceHolder {
    private final Object instance;

    public InstanceHolder(Object instance) {
        this.instance = instance;
    }

    public Object getInstance() {
        return instance;
    }
}
