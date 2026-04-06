package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

@Business(code = "")
public class C3 implements Matcher<Object> {
    @Override
    public boolean match(Object param) {
        return false;
    }
}
