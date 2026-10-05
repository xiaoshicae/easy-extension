package io.github.xiaoshicae.extension.spring.boot.autoconfiguration.scanfixture;

import io.github.xiaoshicae.extension.core.annotation.MatcherParam;

@MatcherParam
public class ScanParam {
    public final String tenant;

    public ScanParam(String tenant) {
        this.tenant = tenant;
    }
}
