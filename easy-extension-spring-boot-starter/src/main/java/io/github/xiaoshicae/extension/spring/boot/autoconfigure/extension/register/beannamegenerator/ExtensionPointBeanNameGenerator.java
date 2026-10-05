package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.util.List;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;

public class ExtensionPointBeanNameGenerator {
    private static final String extensionBeanNameSuffix = "#FirstMatchedExtensionProxy";
    private static final String extensionListBeanNameSuffix = "#AllMatchedExtensionProxy";
    private static final String extensionPointClassHolderBeanNameSuffix = "#ExtensionPointClassHolder";

    public static String genFirstMatchedExtensionBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionBeanNameSuffix;
    }

    public static String genAllMatchedExtensionBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionListBeanNameSuffix;
    }

    public static String genExtensionClassHolderBeanName(String beanClassName) {
        return getClassShortName(beanClassName) + extensionPointClassHolderBeanNameSuffix;
    }

    /**
     * Name of the proxy bean that satisfies an {@code @ExtensionInject} injection point of the given type:
     * {@code List<X>} gets the all-matched proxy of {@code X}, any other type gets its first-matched proxy.
     *
     * @return the bean name, or {@code null} for a {@code List} without a resolvable element type
     */
    public static String genInjectBeanName(ResolvableType type) {
        if (type.resolve() == List.class) {
            Class<?> element = type.getGeneric(0).resolve();
            return element == null ? null : genAllMatchedExtensionBeanName(element.getName());
        }
        return genFirstMatchedExtensionBeanName(type.toClass().getName());
    }

    private static String getClassShortName(String beanClassName) {
        Assert.state(beanClassName != null, "No bean class name set");
        // Use full class name to avoid collision when different packages have same-named interfaces
        return beanClassName;
    }
}
