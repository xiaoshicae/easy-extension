package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;


public class ClassHolder {
    private final Class<?> clazz;

    public ClassHolder(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}

