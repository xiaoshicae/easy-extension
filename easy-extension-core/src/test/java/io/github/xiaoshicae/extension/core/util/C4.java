package io.github.xiaoshicae.extension.core.util;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.common.Matcher;

import java.util.Objects;

@Business(code = "c4", priority = 10, abilities = {"a", "b", "c::10", "d", "e::20", "f"})
public class C4 implements Matcher<Object>, E1, E2, E3 {
    @Override
    public Boolean match(Object param) {
        return Objects.equals(param, "123");
    }

    @Override
    public String doE3() {
        return "C4 doE3";
    }
}
