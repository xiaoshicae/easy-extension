package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Bean names of the infrastructure beans registered per extension point.
 */
public final class ExtensionPointBeanNameGenerator {
    private ExtensionPointBeanNameGenerator() {
    }

    private static final String FIRST_SUFFIX = "#FirstMatchedExtensionProxy";
    private static final String ALL_SUFFIX = "#AllMatchedExtensionProxy";
    private static final String HOLDER_SUFFIX = "#ExtensionPointClassHolder";

    public static String genFirstMatchedExtensionBeanName(String beanClassName) {
        return qualified(beanClassName) + FIRST_SUFFIX;
    }

    public static String genAllMatchedExtensionBeanName(String beanClassName) {
        return qualified(beanClassName) + ALL_SUFFIX;
    }

    public static String genExtensionClassHolderBeanName(String beanClassName) {
        return qualified(beanClassName) + HOLDER_SUFFIX;
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

    // the full class name, so that same-named extension points of different packages do not collide
    private static String qualified(String beanClassName) {
        Assert.state(beanClassName != null, "No bean class name set");
        return beanClassName;
    }
}
