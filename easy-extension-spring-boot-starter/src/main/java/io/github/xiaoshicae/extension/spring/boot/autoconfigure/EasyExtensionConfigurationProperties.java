package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easy-extension")
public class EasyExtensionConfigurationProperties {
    /**
     * 是否启用日志，扩展点匹配过程会打印相应的一致
     * whether to enable logs, the extension point matching process will print log
     */
    private Boolean enableLog = false;


    /**
     * 是否允许未知业务，当没有业务身份可以匹配时，如果不允许未知业务则请求报错，如果允许，则扩展点会走默认能力兜底
     * whether to allow unknown business. when there is no business to match, if not allowed, the request will report an error. If allowed, the extension point will use the default ability.
     */
    private Boolean allowUnknownBusiness = false;

    public Boolean getEnableLog() {
        return enableLog;
    }

    /**
     * @see enableLog
     */
    public void setEnableLog(Boolean enableLog) {
        this.enableLog = enableLog;
    }

    public Boolean getAllowUnknownBusiness() {
        return allowUnknownBusiness;
    }

    /**
     * @see allowUnknownBusiness
     */
    public void setAllowUnknownBusiness(Boolean allowUnknownBusiness) {
        this.allowUnknownBusiness = allowUnknownBusiness;
    }
}
