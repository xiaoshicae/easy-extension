package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.xiaoshicae.extension.core.BaseDefaultAbility;
import io.github.xiaoshicae.extension.core.DefaultExtContext;
import io.github.xiaoshicae.extension.core.IExtContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean.FirstMatchedExtensionFactoryBean;

import java.util.*;

@Configuration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
public class EasyExtensionAutoConfiguration implements BeanFactoryPostProcessor {

    @Bean
    public IExtContext<?> registerExtensionContext(EasyExtensionConfigurationProperties properties, List<IBusiness<?>> businesses, List<IAbility<?>> abilities) throws ExtensionException {
        Boolean enableLog = properties.getEnableLog();
        Boolean allowUnknownBusiness = properties.getAllowUnknownBusiness();

        checkBusinessAbilityPriority(businesses);

        DefaultExtContext<?> manager = new DefaultExtContext<>(enableLog, true, !allowUnknownBusiness);

        for (IBusiness business : businesses) {
            manager.registerBusiness(business);
        }

        for (IAbility ability : abilities) {
            manager.registerAbility(ability);
        }

        return manager;
    }

    private void checkBusinessAbilityPriority(List<IBusiness<?>> businesses) throws ExtensionException {
        for (IBusiness<?> business : businesses) {
            Set<Integer> set = new HashSet<>();
            set.add(business.priority());
            for (UsedAbility usedAbility : business.usedAbilities()) {
                if (set.contains(usedAbility.getPriority())) {
                    throw new ExtensionException("business " + business.code() + " priority conflict, business and used ability can not take the same priority");
                }
                set.add(usedAbility.getPriority());
            }
        }
    }

    // after bean definition collected, check default ability must implements all extension interface
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        List<Class<?>> extensionClazzList = new ArrayList<>();
        Class<?> defaultAbilityClazz = null;
        int defaultAbilityCnt = 0;

        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition;
            String beanClassName;
            try {
                beanDefinition = beanFactory.getBeanDefinition(beanName);
                beanClassName = beanDefinition.getBeanClassName();
            } catch (Exception e) {
                continue;
            }
            if (beanClassName == null) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(beanClassName);
                if (BaseDefaultAbility.class.isAssignableFrom(clazz)) {
                    defaultAbilityClazz = clazz;
                    defaultAbilityCnt++;
                }
                if (FirstMatchedExtensionFactoryBean.class == clazz) {
                    String extensionClassName = (String) beanDefinition.getConstructorArgumentValues().getGenericArgumentValues().get(0).getValue();
                    Class<?> extensionClazz = Class.forName(extensionClassName);
                    extensionClazzList.add(extensionClazz);
                }
            } catch (Exception ignored) {
            }
        }

        if (defaultAbilityCnt == 0) {
            throw new BeanCreationException("default ability not found");
        }

        if (defaultAbilityCnt > 1) {
            throw new BeanCreationException("multiple default ability found");
        }

        List<String> notImplemented = new ArrayList<>();
        for (Class<?> clazz : extensionClazzList) {
            if (!clazz.isAssignableFrom(defaultAbilityClazz)) {
                notImplemented.add(clazz.getName());
            }
        }
        if (!notImplemented.isEmpty()) {
            // 默认能力应该实现所有扩展点接口，但是当前实例没有实现XXX扩展点
            throw new BeanCreationException("default ability should implements all extension interface, but current default ability not implements extension " + Arrays.toString(notImplemented.toArray()));
        }
    }
}
