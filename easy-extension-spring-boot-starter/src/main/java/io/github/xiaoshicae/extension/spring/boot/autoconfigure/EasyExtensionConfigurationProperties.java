package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.business.MultiMatchPolicy;
import io.github.xiaoshicae.extension.core.business.UnknownBusinessPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "easy-extension")
public class EasyExtensionConfigurationProperties {
    /**
     * 是否启用日志，扩展点匹配过程会打印相应的日志
     * whether to enable logs, the extension point matching process will print log
     */
    private boolean enableLog = false;


    /**
     * 是否允许未知业务（旧开关）：只决定没有显式配置的 unknown-business-policy / multi-match-policy。false 等于两者都是 reject，true 等于 default + select。
     * Legacy switch for the two policies, it only decides the ones that are not set explicitly: false means unknown-business-policy and multi-match-policy are both reject
     * (a request that matches no business, or several, fails), true means default and select (the default implementations serve it, one of several is picked).
     */
    private boolean allowUnknownBusiness = false;

    /**
     * 没有任何业务匹配时怎么办：reject 请求报错（no business matched），default 走默认实现兜底。不配置时跟随 allow-unknown-business。
     * what to do when no business matches: reject fails the request (no business matched), default serves it with the default implementations.
     * When not set, follows allow-unknown-business.
     */
    private UnknownBusinessPolicy unknownBusinessPolicy;

    /**
     * 多个业务同时匹配时怎么办：reject 请求报错（multiple business found），select 由 business-match-order / BusinessMatchSelector 选一个（并打印一次 WARN）。不配置时跟随 allow-unknown-business。
     * what to do when several businesses match: reject fails the request (multiple business found), select lets business-match-order / BusinessMatchSelector pick one
     * (and logs a WARN once per combination). When not set, follows allow-unknown-business.
     */
    private MultiMatchPolicy multiMatchPolicy;

    /**
     * 是否启用Session自动清理过滤器，在Web请求结束后自动清理ThreadLocal中的Session数据，防止内存泄漏
     * whether to enable the session auto cleanup filter, which automatically cleans up session data in ThreadLocal after the web request ends to prevent memory leaks.
     */
    private boolean enableSessionAutoCleanup = true;

    /**
     * 业务匹配优先级顺序，使用业务 code。多个业务同时匹配且 multi-match-policy 为 select 时，按此顺序选择排在最前的。
     * 未列出的业务按注册顺序排在末尾。multi-match-policy 为 reject 时不使用。
     * Business match priority order by business code. When several businesses match and multi-match-policy is select,
     * the one appearing first in this list wins; businesses not listed come last, in registration order. Not used when multi-match-policy is reject.
     */
    private List<String> businessMatchOrder = List.of();

    public boolean getEnableLog() {
        return enableLog;
    }

    public void setEnableLog(boolean enableLog) {
        this.enableLog = enableLog;
    }

    public boolean getAllowUnknownBusiness() {
        return allowUnknownBusiness;
    }

    public void setAllowUnknownBusiness(boolean allowUnknownBusiness) {
        this.allowUnknownBusiness = allowUnknownBusiness;
    }

    public UnknownBusinessPolicy getUnknownBusinessPolicy() {
        return unknownBusinessPolicy;
    }

    public void setUnknownBusinessPolicy(UnknownBusinessPolicy unknownBusinessPolicy) {
        this.unknownBusinessPolicy = unknownBusinessPolicy;
    }

    public MultiMatchPolicy getMultiMatchPolicy() {
        return multiMatchPolicy;
    }

    public void setMultiMatchPolicy(MultiMatchPolicy multiMatchPolicy) {
        this.multiMatchPolicy = multiMatchPolicy;
    }

    /**
     * The policy in force for a request that matches no business: the explicit
     * {@code unknown-business-policy}, else what {@code allow-unknown-business} stands for.
     */
    public UnknownBusinessPolicy effectiveUnknownBusinessPolicy() {
        if (unknownBusinessPolicy != null) {
            return unknownBusinessPolicy;
        }
        return allowUnknownBusiness ? UnknownBusinessPolicy.DEFAULT : UnknownBusinessPolicy.REJECT;
    }

    /**
     * The policy in force for a request that matches several businesses: the explicit
     * {@code multi-match-policy}, else what {@code allow-unknown-business} stands for.
     */
    public MultiMatchPolicy effectiveMultiMatchPolicy() {
        if (multiMatchPolicy != null) {
            return multiMatchPolicy;
        }
        return allowUnknownBusiness ? MultiMatchPolicy.SELECT : MultiMatchPolicy.REJECT;
    }

    public boolean getEnableSessionAutoCleanup() {
        return enableSessionAutoCleanup;
    }

    public void setEnableSessionAutoCleanup(boolean enableSessionAutoCleanup) {
        this.enableSessionAutoCleanup = enableSessionAutoCleanup;
    }

    public List<String> getBusinessMatchOrder() {
        return businessMatchOrder;
    }

    public void setBusinessMatchOrder(List<String> businessMatchOrder) {
        this.businessMatchOrder = businessMatchOrder;
    }
}
