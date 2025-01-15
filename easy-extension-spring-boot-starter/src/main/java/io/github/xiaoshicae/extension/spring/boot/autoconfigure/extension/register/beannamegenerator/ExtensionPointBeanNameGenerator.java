package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;

public class ExtensionPointBeanNameGenerator {
    private static final String extensionBeanNameSuffix = "#FirstMatchedExtensionProxy";
    private static final String extensionListBeanNameSuffix = "#AllMatchedExtensionProxy";
    private static final String extensionProxyFactoryBeanNameSuffix = "#MatchedExtensionProxyFactory";


    public static String genFirstMatchedExtensionBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionBeanNameSuffix;
    }

    public static String genAllMatchedExtensionBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionListBeanNameSuffix;
    }

    public static String genExtensionProxyFactoryBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionProxyFactoryBeanNameSuffix;
    }

    private static String getClassShortName(String beanClassName) {
        Assert.state(beanClassName != null, "No bean class name set");
        String shortClassName = ClassUtils.getShortName(beanClassName);
        return Introspector.decapitalize(shortClassName);
    }
}
