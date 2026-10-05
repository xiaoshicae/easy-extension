package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture;

import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;

import java.util.List;

/** Annotated AND implements the framework interface (via the abstract base class). */
@Business(code = "iface.business")
public class IfaceBusiness extends AbstractBusiness<IfaceParam> implements IfacePay {
    @Override
    public String code() {
        return "iface.business";
    }

    @Override
    public boolean match(IfaceParam param) {
        return true;
    }

    @Override
    public Integer priority() {
        return 10;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("iface.ability", 5));
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IfacePay.class);
    }

    @Override
    public String pay() {
        return "business";
    }
}
