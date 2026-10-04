package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
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
        String[] packages = StringUtils.tokenizeToStringArray(getScanPackages(), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

        // scan @ExtensionPoint
        configure(new ExtensionPointScanner(registry)).scan(packages);

        // scan @MatcherParam
        configure(new ClassScanner(registry)).scan(packages);

        // scan @Ability, @Business, @ExtensionPointDefaultImplementation ...
        configure(new InstanceScanner(registry)).scan(packages);
    }

    private <S extends ClassPathBeanDefinitionScanner> S configure(S scanner) {
        scanner.setResourceLoader(getApplicationContext());
        // A scanner made from a bare bean definition registry makes an environment of its own, one that knows neither the
        // active profiles nor the properties of the application: @Profile and @ConditionalOnProperty on a scanned class
        // would be decided by that one.
        scanner.setEnvironment(getApplicationContext().getEnvironment());
        return scanner;
    }
}
