package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import static org.springframework.util.Assert.notNull;

public class ExtensionScannerConfigurer implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware {
    private String scanPackages;
    private ApplicationContext applicationContext;

    public String getScanPackages() {
        return scanPackages;
    }

    public void setScanPackages(String scanPackages) {
        this.scanPackages = scanPackages;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        notNull(this.scanPackages, "Property 'scanPackages' is required");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // left intentionally blank
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // scan @ExtensionPoint
        ExtensionPointScanner extensionPointScanner = new ExtensionPointScanner(registry);
        extensionPointScanner.setResourceLoader(getApplicationContext());
        extensionPointScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @ExtensionPointDefaultImplementation
        ExtensionPointDefaultImplScanner extDefaultScanner = new ExtensionPointDefaultImplScanner(registry);
        extDefaultScanner.setResourceLoader(getApplicationContext());
        extDefaultScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @MatcherParam
        MatcherParamScanner matcherParamScanner = new MatcherParamScanner(registry);
        matcherParamScanner.setResourceLoader(getApplicationContext());
        matcherParamScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @Ability
        AbilityScanner abilityScanner = new AbilityScanner(registry);
        abilityScanner.setResourceLoader(getApplicationContext());
        abilityScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @Business
        BusinessScanner businessScanner = new BusinessScanner(registry);
        businessScanner.setResourceLoader(getApplicationContext());
        businessScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }
}
