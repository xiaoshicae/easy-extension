package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;

import java.util.concurrent.atomic.AtomicInteger;

@ExtensionPointDefaultImplementation
public class ScanDefaultPay implements ScanPay {
    public static final AtomicInteger INSTANCES = new AtomicInteger();

    public ScanDefaultPay() {
        INSTANCES.incrementAndGet();
    }

    @Override
    public String pay() {
        return "default";
    }
}
