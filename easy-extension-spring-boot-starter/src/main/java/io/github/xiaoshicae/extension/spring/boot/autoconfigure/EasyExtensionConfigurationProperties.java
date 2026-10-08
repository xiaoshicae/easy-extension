package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Properties under {@code easy-extension.*}.
 */
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

    /**
     * 不绑定业务身份的请求路径（Ant 风格），例如健康检查。严格模式下，匹配不到业务的请求会直接报错，这些路径不受影响。
     * Request path patterns (Ant style) that never get a business bound, e.g. health checks. In strict mode a request no business matches fails, these paths are not affected.
     */
    private List<String> sessionExcludePathPatterns = List.of();

    /**
     * 只对这些请求路径（Ant 风格）绑定业务身份，缺省为全部路径（/**）。与 session-exclude-path-patterns 同时配置时，先包含再排除。
     * Request path patterns (Ant style) the business is bound for; all paths (/**) when empty. Combined with session-exclude-path-patterns: included first, then excluded.
     */
    private List<String> sessionIncludePathPatterns = List.of();

    /**
     * 是否把请求线程上的绑定传递给 Spring 管理的异步任务线程池（@Async、applicationTaskExecutor），任务沿用提交时的业务身份。缺省关闭。
     * Whether the binding of the submitting thread follows tasks onto Spring-managed task executors (@Async, applicationTaskExecutor). Off by default.
     */
    private boolean asyncPropagation = false;

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

    public List<String> getSessionExcludePathPatterns() {
        return sessionExcludePathPatterns;
    }

    public void setSessionExcludePathPatterns(List<String> sessionExcludePathPatterns) {
        this.sessionExcludePathPatterns = sessionExcludePathPatterns == null ? List.of() : List.copyOf(sessionExcludePathPatterns);
    }

    public List<String> getSessionIncludePathPatterns() {
        return sessionIncludePathPatterns;
    }

    public void setSessionIncludePathPatterns(List<String> sessionIncludePathPatterns) {
        this.sessionIncludePathPatterns = sessionIncludePathPatterns == null ? List.of() : List.copyOf(sessionIncludePathPatterns);
    }

    public boolean isAsyncPropagation() {
        return asyncPropagation;
    }

    public void setAsyncPropagation(boolean asyncPropagation) {
        this.asyncPropagation = asyncPropagation;
    }
}
