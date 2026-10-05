package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ClassScanner;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionComponentScanner;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointScanner;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.InstanceScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionComponentScannerTest {

    private static final String PACKAGE = "io.github.xiaoshicae.extension.spring.boot.autoconfiguration";

    @Test
    public void testSinglePassRegistersExactlyWhatTheSpecialisedScannersRegister() {
        DefaultListableBeanFactory perKind = new DefaultListableBeanFactory();
        new ExtensionPointScanner(perKind).scan(PACKAGE);
        new ClassScanner(perKind).scan(PACKAGE);
        new InstanceScanner(perKind).scan(PACKAGE);

        DefaultListableBeanFactory singlePass = new DefaultListableBeanFactory();
        new ExtensionComponentScanner(singlePass).scan(PACKAGE);

        Map<String, String> expected = describe(perKind);
        assertTrue(expected.keySet().stream().anyMatch(name -> name.endsWith("#FirstMatchedExtensionProxy")), "extension points scanned");
        assertTrue(expected.keySet().stream().anyMatch(name -> name.endsWith("#ClassHolder")), "matcher param scanned");
        assertTrue(expected.keySet().stream().anyMatch(name -> name.endsWith("#InstanceHolder")), "abilities/businesses/default impl scanned");
        assertEquals(expected, describe(singlePass));
    }

    /** bean name -> bean class + constructor arguments, which is everything the registration decides. */
    private static Map<String, String> describe(DefaultListableBeanFactory beanFactory) {
        Map<String, String> description = new TreeMap<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);
            String args = definition.getConstructorArgumentValues().getIndexedArgumentValues().values().stream()
                    .map(value -> String.valueOf(value.getValue()))
                    .toList().toString();
            description.put(name, definition.getBeanClassName() + args);
        }
        return description;
    }
}
