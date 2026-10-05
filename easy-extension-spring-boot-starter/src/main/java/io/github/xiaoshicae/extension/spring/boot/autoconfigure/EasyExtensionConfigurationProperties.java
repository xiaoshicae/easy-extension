package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "easy-extension")
public class EasyExtensionConfigurationProperties {
    /**
     * 是否允许未知业务，当没有业务身份可以匹配时，如果不允许未知业务则请求报错，如果允许，则扩展点会走默认实现兜底
     * whether to allow unknown business. when there is no business to match, if not allowed, the request will report an error. If allowed, only default implementations answer.
     */
    private boolean allowUnknownBusiness = false;

    /**
     * 是否启用Session兜底清理过滤器，在Web请求结束后清理当前线程上残留的绑定，防止线程池场景下的内存泄漏
     * whether to enable the safety-net filter that drops any binding left on the thread when a web request ends.
     */
    private boolean enableSessionAutoCleanup = true;

    /**
     * 业务匹配顺序（使用业务 code）。当多个业务同时匹配（且不要求严格匹配）时，按此顺序选择；未列出的取先注册的。
     * Business codes in selection order, consulted when several businesses match and unknown businesses are allowed.
     */
    private List<String> businessMatchOrder = List.of();

    /**
     * 匹配参数类型。缺省时由能力/业务实现的 Matcher&lt;T&gt; 泛型推导。
     * Type of the request parameter. Derived from the Matcher&lt;T&gt; generics of abilities and businesses when not set.
     */
    private Class<?> matcherParamType;

    public boolean isAllowUnknownBusiness() {
        return allowUnknownBusiness;
    }

    public void setAllowUnknownBusiness(boolean allowUnknownBusiness) {
        this.allowUnknownBusiness = allowUnknownBusiness;
    }

    public boolean isEnableSessionAutoCleanup() {
        return enableSessionAutoCleanup;
    }

    public void setEnableSessionAutoCleanup(boolean enableSessionAutoCleanup) {
        this.enableSessionAutoCleanup = enableSessionAutoCleanup;
    }

    public List<String> getBusinessMatchOrder() {
        return businessMatchOrder;
    }

    public void setBusinessMatchOrder(List<String> businessMatchOrder) {
        this.businessMatchOrder = businessMatchOrder == null ? List.of() : businessMatchOrder;
    }

    public Class<?> getMatcherParamType() {
        return matcherParamType;
    }

    public void setMatcherParamType(Class<?> matcherParamType) {
        this.matcherParamType = matcherParamType;
    }
}
