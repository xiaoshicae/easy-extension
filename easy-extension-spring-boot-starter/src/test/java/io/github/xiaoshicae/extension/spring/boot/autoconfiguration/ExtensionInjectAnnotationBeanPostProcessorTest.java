package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.annotation.ExtensionInject;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.postprocessor.ExtensionInjectAnnotationBeanPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link ExtensionInjectAnnotationBeanPostProcessor} wires up
 * {@code @ExtensionInject}-annotated fields from the {@link DefaultListableBeanFactory}
 * using the bean names produced by {@link ExtensionPointBeanNameGenerator}.
 */
class ExtensionInjectAnnotationBeanPostProcessorTest {

    interface FreightExt {
        String calc();
    }

    static class FreightExtImpl implements FreightExt {
        @Override
        public String calc() {
            return "free-shipping";
        }
    }

    static class OrderService {
        @ExtensionInject
        FreightExt freight;

        @ExtensionInject
        List<FreightExt> allFreight;
    }

    static class StaticFieldBean {
        @ExtensionInject
        static FreightExt freight;
    }

    static class MissingGenericBean {
        @ExtensionInject
        @SuppressWarnings("rawtypes")
        List rawList;
    }

    @Test
    void injectsSingleAndListFieldsFromBeanFactory() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        FreightExt single = new FreightExtImpl();
        List<FreightExt> all = List.of(new FreightExtImpl());
        bf.registerSingleton(
                ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(FreightExt.class.getName()),
                single);
        bf.registerSingleton(
                ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(FreightExt.class.getName()),
                all);

        ExtensionInjectAnnotationBeanPostProcessor bpp = new ExtensionInjectAnnotationBeanPostProcessor();
        bpp.setBeanFactory(bf);

        OrderService bean = new OrderService();
        bpp.postProcessProperties(new MutablePropertyValues(), bean, "orderService");

        assertNotNull(bean.freight, "single @ExtensionInject field should be populated");
        assertEquals("free-shipping", bean.freight.calc());
        assertNotNull(bean.allFreight, "list @ExtensionInject field should be populated");
        assertEquals(1, bean.allFreight.size());
    }

    @Test
    void failsWithActionableMessageWhenBeanMissing() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        ExtensionInjectAnnotationBeanPostProcessor bpp = new ExtensionInjectAnnotationBeanPostProcessor();
        bpp.setBeanFactory(bf);

        OrderService bean = new OrderService();
        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> bpp.postProcessProperties(new MutablePropertyValues(), bean, "orderService"));
        String rootMsg = rootMessage(ex);
        assertTrue(rootMsg.contains("@ExtensionScan") || rootMsg.contains("no bean"),
                "missing-bean error should guide the user, got: " + rootMsg);
    }

    @Test
    void rejectsStaticFields() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        ExtensionInjectAnnotationBeanPostProcessor bpp = new ExtensionInjectAnnotationBeanPostProcessor();
        bpp.setBeanFactory(bf);

        StaticFieldBean bean = new StaticFieldBean();
        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> bpp.postProcessProperties(new MutablePropertyValues(), bean, "staticFieldBean"));
        String rootMsg = rootMessage(ex);
        assertTrue(rootMsg.contains("static"),
                "static-field rejection expected, got: " + rootMsg);
    }

    @Test
    void rejectsRawListWithoutGeneric() {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        ExtensionInjectAnnotationBeanPostProcessor bpp = new ExtensionInjectAnnotationBeanPostProcessor();
        bpp.setBeanFactory(bf);

        MissingGenericBean bean = new MissingGenericBean();
        BeanCreationException ex = assertThrows(BeanCreationException.class,
                () -> bpp.postProcessProperties(new MutablePropertyValues(), bean, "missingGeneric"));
        String rootMsg = rootMessage(ex);
        assertTrue(rootMsg.contains("generic"),
                "raw-List rejection expected, got: " + rootMsg);
    }

    /** Concatenate every message in the causal chain so assertions don't depend on wrapping depth. */
    private static String rootMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
            if (cur.getMessage() != null) {
                sb.append(cur.getMessage()).append('\n');
            }
        }
        return sb.toString();
    }
}
