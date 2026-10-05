package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionComponentScanner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans the registered packages. There is exactly one of these per context; {@link #registerScanPackages} adds packages to it
 * from wherever they come (auto-configuration packages, {@code @ExtensionScan}).
 */
public class ExtensionScannerConfigurer implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    static final String BEAN_NAME = ExtensionScannerConfigurer.class.getName();

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

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        ExtensionComponentScanner scanner = new ExtensionComponentScanner(registry);
        scanner.setResourceLoader(applicationContext);
        scanner.scan(StringUtils.tokenizeToStringArray(scanPackages, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    /**
     * Adds packages to the scan; a package already covered by a parent package is dropped.
     */
    static void registerScanPackages(BeanDefinitionRegistry registry, Collection<String> packages) {
        Set<String> merged = new LinkedHashSet<>();
        if (registry.containsBeanDefinition(BEAN_NAME)) {
            Object existing = registry.getBeanDefinition(BEAN_NAME).getPropertyValues().get("scanPackages");
            if (existing != null) {
                merged.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(existing.toString())));
            }
        }
        packages.stream().filter(StringUtils::hasText).forEach(merged::add);
        if (merged.isEmpty()) {
            return;
        }

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionScannerConfigurer.class)
                .addPropertyValue("scanPackages", StringUtils.collectionToCommaDelimitedString(withoutNestedPackages(merged)))
                .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (registry.containsBeanDefinition(BEAN_NAME)) {
            registry.removeBeanDefinition(BEAN_NAME);
        }
        registry.registerBeanDefinition(BEAN_NAME, builder.getBeanDefinition());
    }

    private static List<String> withoutNestedPackages(Set<String> packages) {
        List<String> result = new ArrayList<>();
        for (String candidate : packages) {
            boolean covered = packages.stream().anyMatch(other -> !other.equals(candidate) && candidate.startsWith(other + "."));
            if (!covered) {
                result.add(candidate);
            }
        }
        return result;
    }
}
