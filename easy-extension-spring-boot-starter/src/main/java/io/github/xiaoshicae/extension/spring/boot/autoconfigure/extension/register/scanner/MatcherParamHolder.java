package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner;

public class MatcherParamHolder<T> {
    private final Class<T> matcherParamClass;

    public MatcherParamHolder(Class<T> matcherParamClass) {
        this.matcherParamClass = matcherParamClass;
    }

    public Class<T> getMatcherParamClass() {
        return matcherParamClass;
    }
}
