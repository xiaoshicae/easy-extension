package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register;

import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.proxy.ExtensionProxyFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import io.github.xiaoshicae.extension.core.IExtFactory;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.AllMatchedExtensionFactoryBean;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by {@code basePackage} or {@code annotationClass}.
 * If an {@code annotationClass} is specified, only the specified types will be searched (searching for all interfaces will be disabled).
 *
 * @see FirstMatchedExtensionFactoryBean
 */
public class ClassPathExtensionScanner extends ClassPathBeanDefinitionScanner {

    private static final Logger LOGGER = Logger.getLogger(ClassPathExtensionScanner.class.getName());

    private static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

    private static final Class<? extends Annotation> annotationClass = ExtensionPoint.class;

    private final ExtensionBeanNameGenerator extensionBeanNameGenerator = ExtensionBeanNameGenerator.INSTANCE;

    public ClassPathExtensionScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    /**
     * Configures parent scanner to search for the right interfaces. It can search for all interfaces or just for those
     * that extends a markerInterface or/and those annotated with the annotationClass
     */
    public void registerFilters() {
        addIncludeFilter(new AnnotationTypeFilter(annotationClass));
    }

    /**
     * Calls the parent search that will search and register all the candidates. Then the registered objects are post
     * processed to set them as MapperFactoryBeans
     */
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
        if (beanDefinitions.isEmpty()) {
            LOGGER.warning(() -> "No lemon Extension was found in '" + Arrays.toString(basePackages) + "' package. Please check your configuration.");
        } else {
            processBeanDefinitions(beanDefinitions);
        }
        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition extDefinition;
        BeanDefinitionRegistry registry = getRegistry();
        for (BeanDefinitionHolder holder : beanDefinitions) {
            extDefinition = (AbstractBeanDefinition) holder.getBeanDefinition();
            // extension origin interface class name
            String beanClassName = extDefinition.getBeanClassName();
            String extensionProxyFactoryBeanName = this.extensionBeanNameGenerator.genExtensionProxyFactoryBeanName(beanClassName);
            LOGGER.config(() -> "Creating ExtensionFactoryBean with name '" + holder.getBeanName() + "' and '" + beanClassName + "' extensionInterface");
            // the extension interface is the original class of the bean
            // but, the actual class of the bean is ExtensionFactoryBean
            extDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
            extDefinition.setBeanClass(FirstMatchedExtensionFactoryBean.class);
            // Attribute for MockitoPostProcessor
            // https://github.com/mybatis/spring-boot-starter/issues/475
            try {
                Class<?> beanClass = Class.forName(beanClassName);
                extDefinition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClass);
                extDefinition.getPropertyValues().add("extensionProxyFactory", new RuntimeBeanReference(extensionProxyFactoryBeanName));
            } catch (ClassNotFoundException e) {
                // ignore
            }

            // register all matched extensions bean definition
            BeanDefinition extensionListBeanDefinition = buildExtensionListBeanDefinition(beanClassName, extensionProxyFactoryBeanName);
            String extensionListBeanName = this.extensionBeanNameGenerator.genExtensionListBeanName(beanClassName);
//            extensionListBeanDefinition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, extensionListBeanName);
            registry.registerBeanDefinition(extensionListBeanName, extensionListBeanDefinition);

            // register extension proxy factory bean definition
            BeanDefinition extensionProxyFactoryBeanDefinition = buildExtensionProxyFactoryBeanDefinition(beanClassName, extensionProxyFactoryBeanName);
//            extensionProxyFactoryBeanDefinition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, extensionProxyFactoryBeanName);
            registry.registerBeanDefinition(extensionProxyFactoryBeanName, extensionProxyFactoryBeanDefinition);
        }
    }

    private BeanDefinition buildExtensionListBeanDefinition(String ctorBeanClassName, String extensionProxyFactoryBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AllMatchedExtensionFactoryBean.class);
        builder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(ctorBeanClassName);
        builder.addPropertyValue("extensionProxyFactory", new RuntimeBeanReference(extensionProxyFactoryBeanName));
        return builder.getBeanDefinition();
    }

    private BeanDefinition buildExtensionProxyFactoryBeanDefinition(String ctorBeanClassName, String extensionProxyFactoryBeanName) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ExtensionProxyFactory.class);
        builder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(ctorBeanClassName);
        builder.addPropertyValue("extFactory", new RuntimeBeanReference(IExtFactory.class));
        return builder.getBeanDefinition();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        } else {
            LOGGER.config(() -> "Skipping ExtensionFactoryBean with name '" + beanName + "' and '" + beanDefinition.getBeanClassName() + "' extensionInterface" + ". Bean already defined with the same name!");
            return false;
        }
    }
}
