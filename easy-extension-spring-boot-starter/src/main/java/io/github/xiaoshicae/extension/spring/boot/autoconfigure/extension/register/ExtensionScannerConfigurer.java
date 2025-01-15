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
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // scan @ExtensionPoint
        ExtensionPointScanner extensionPointScanner = new ExtensionPointScanner(registry);
        extensionPointScanner.setResourceLoader(getApplicationContext());
        extensionPointScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @MatcherParam
        ClassScanner classScanner = new ClassScanner(registry);
        classScanner.setResourceLoader(getApplicationContext());
        classScanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));

        // scan @Ability, @Business, @ExtensionPointDefaultImplementation ...
        InstanceScanner scanner = new InstanceScanner(registry);
        scanner.setResourceLoader(getApplicationContext());
        scanner.scan(StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }
}
