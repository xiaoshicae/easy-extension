package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;

public class ExtensionBeanNameGenerator implements BeanNameGenerator {
    // bean name suffix for first matched extension proxy
    private static final String extensionBeanNameSuffix = "#Extension#Proxy";

    // bean name suffix for all matched extension proxy
    private static final String extensionListBeanNameSuffix = "#ExtensionList#Proxy";

    // bean name suffix for proxy factory
    private static final String extensionProxyFactoryBeanNameSuffix = "#Extension#ProxyFactory";

    // singleton instance
    public static final ExtensionBeanNameGenerator INSTANCE = new ExtensionBeanNameGenerator();

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        String beanClassName = definition.getBeanClassName();
        return genExtensionBeanName(beanClassName);
    }

    public String genExtensionBeanName(String beanClassName) {
        return genExtensionBeanNameFromClassName(beanClassName) + extensionBeanNameSuffix;
    }

    public String genExtensionListBeanName(String beanClassName) {
        return genExtensionBeanNameFromClassName(beanClassName) + extensionListBeanNameSuffix;
    }

    public String genExtensionProxyFactoryBeanName(String beanClassName) {
        return genExtensionBeanNameFromClassName(beanClassName) + extensionProxyFactoryBeanNameSuffix;
    }

    private String genExtensionBeanNameFromClassName(String beanClassName) {
        Assert.state(beanClassName != null, "No bean class name set");
        String shortClassName = ClassUtils.getShortName(beanClassName);
        return Introspector.decapitalize(shortClassName);
    }
}
