package io.github.xiaoshicae.extension.spring.boot.autoconfiguration;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.beannamegenerator.ExtensionPointBeanNameGenerator;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionComponentScanner;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture.Domain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionComponentScannerTest {
    private static final String PACKAGE = "io.github.xiaoshicae.extension.spring.boot.autoconfiguration.fixture";

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    {
        new ExtensionComponentScanner(beanFactory).scan(PACKAGE);
    }

    @Test
    public void testExtensionPointsGetAHolderAndTheTwoInjectableProxies() {
        for (Class<?> point : new Class<?>[]{Domain.Pay.class, Domain.Ship.class}) {
            String name = point.getName();
            BeanDefinition first = beanFactory.getBeanDefinition(ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(name));
            assertEquals(FirstMatchedExtensionFactoryBean.class.getName(), first.getBeanClassName());
            assertTrue(first.isPrimary(), "plain by-type injection of the extension point should get the proxy");
            assertEquals(AllMatchedExtensionFactoryBean.class.getName(),
                    beanFactory.getBeanDefinition(ExtensionPointBeanNameGenerator.genAllMatchedExtensionBeanName(name)).getBeanClassName());
            assertEquals(ExtensionPointHolder.class.getName(),
                    beanFactory.getBeanDefinition(ExtensionPointBeanNameGenerator.genExtensionClassHolderBeanName(name)).getBeanClassName());
        }
    }

    @Test
    public void testProvidersAreRegisteredAsOrdinaryBeans() {
        for (Class<?> provider : new Class<?>[]{Domain.FastShipAbility.class, Domain.ComposedAbility.class, Domain.RetailBusiness.class,
                Domain.ComposedBusiness.class, Domain.DefaultPay.class, Domain.DefaultShip.class}) {
            assertEquals(1, beanFactory.getBeanNamesForType(provider, true, false).length, provider.getName());
        }
    }

    @Test
    public void testOnlyExtensionPointsProvidersAndNothingElseIsRegistered() {
        // an @ExtensionPoint on a class is not an extension point, and plain classes are not providers
        assertEquals(0, beanFactory.getBeanNamesForType(Domain.NotAnExtensionPoint.class, true, false).length);
        assertEquals(0, beanFactory.getBeanNamesForType(Domain.OrderService.class, true, false).length);
        assertFalse(beanFactory.containsBeanDefinition(
                ExtensionPointBeanNameGenerator.genFirstMatchedExtensionBeanName(Domain.NotAnExtensionPoint.class.getName())));
    }

    @Test
    public void testScanningTheSamePackageTwiceDoesNotRegisterTwice() {
        int before = beanFactory.getBeanDefinitionCount();
        new ExtensionComponentScanner(beanFactory).scan(PACKAGE);
        assertEquals(before, beanFactory.getBeanDefinitionCount());
    }
}
