package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture;

import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;

import java.util.List;

/** Annotated AND implements the framework interface (via the abstract base class). */
@Ability(code = "iface.ability")
public class IfaceAbility extends AbstractAbility<IfaceParam> implements IfacePay {
    @Override
    public String code() {
        return "iface.ability";
    }

    @Override
    public boolean match(IfaceParam param) {
        return true;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IfacePay.class);
    }

    @Override
    public String pay() {
        return "ability";
    }
}
