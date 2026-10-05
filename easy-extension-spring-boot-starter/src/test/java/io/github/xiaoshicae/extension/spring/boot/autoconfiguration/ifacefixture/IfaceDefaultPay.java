package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.ifacefixture;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.extension.AbstractExtensionPointDefaultImplementation;

import java.util.List;

/** Annotated AND implements the framework interface (via the abstract base class). */
@ExtensionPointDefaultImplementation
public class IfaceDefaultPay extends AbstractExtensionPointDefaultImplementation<IfaceParam> implements IfacePay {
    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of(IfacePay.class);
    }

    @Override
    public String pay() {
        return "default";
    }
}
