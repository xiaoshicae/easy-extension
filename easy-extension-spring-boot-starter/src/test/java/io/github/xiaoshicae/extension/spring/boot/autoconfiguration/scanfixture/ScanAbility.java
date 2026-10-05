package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;

import java.util.concurrent.atomic.AtomicInteger;

@Ability(code = "scan.ability")
public class ScanAbility implements Matcher<ScanParam>, ScanPay {
    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public ScanAbility() {
        INSTANCES.incrementAndGet();
    }

    @Override
    public boolean match(ScanParam param) {
        return true;
    }

    @Override
    public String pay() {
        return "ability";
    }
}
