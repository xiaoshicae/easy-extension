package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.annotation.MatcherParam;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.interfaces.Matcher;
import io.github.xiaoshicae.extension.core.exception.ProxyException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.util.ExtensionContextRegisterByAnnHelper;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ClassHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.InstanceHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;


@Configuration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
public class EasyExtensionAutoConfiguration<T> {
    private static final Logger logger = Logger.getLogger(EasyExtensionAutoConfiguration.class.getName());

    private List<ExtensionPointHolder> extensionPointHolders;
    private IExtensionPointGroupDefaultImplementation<T> extensionPointGroupImplementation;
    private List<IBusiness<T>> businesses;
    private List<IAbility<T>> abilities;
    private List<InstanceHolder> instanceHolders;
    private List<ClassHolder> classHolders;

    @Bean
    public IExtensionContext<T> registerExtensionContext(EasyExtensionConfigurationProperties properties) throws RegisterException, ProxyException {
        IExtensionContext<T> extensionContext = new DefaultExtensionContext<>(properties.getEnableLog(), !properties.getAllowUnknownBusiness());
        ExtensionContextRegisterByAnnHelper<T> helper = new ExtensionContextRegisterByAnnHelper<>(extensionContext);

        // if no extension point found, return empty context
        if (Objects.isNull(this.extensionPointHolders) || this.extensionPointHolders.isEmpty()) {
            if (properties.getEnableLog()) {
                logger.info("No extension point found. Please check your configuration.");
            }
            return extensionContext;
        }

        registerExtensionPoint(helper);

        registerMatcherParamClass(helper);

        registerExtensionPointGroupImpl(helper);

        helper.doRegister();

        return extensionContext;
    }

    private void registerExtensionPoint(ExtensionContextRegisterByAnnHelper<T> helper) {
        for (ExtensionPointHolder holder : this.extensionPointHolders) {
            helper.addExtensionPointClasses(holder.getExtensionPointClass());
        }
    }

    @SuppressWarnings("unchecked")
    private void registerMatcherParamClass(ExtensionContextRegisterByAnnHelper<T> helper) throws RegisterParamException {
        if (Objects.isNull(this.classHolders) || this.classHolders.isEmpty()) {
            throw new RegisterParamException("instance annotated with @MatcherParam not found");
        }

        List<Class<?>> matcherParamClasses = new ArrayList<>();
        for (ClassHolder classHolder : this.classHolders) {
            Class<?> clazz = classHolder.getClazz();
            if (clazz.isAnnotationPresent(MatcherParam.class)) {
                matcherParamClasses.add(clazz);
            }
        }
        if (matcherParamClasses.size() > 1) {
            throw new RegisterParamException("More than one instance annotated with @MatcherParam found");
        }
        helper.setMatcherParamClass((Class<T>) matcherParamClasses.get(0));
    }

    /**
     * register extension implementation, abilities, businesses
     */
    @SuppressWarnings("unchecked")
    private void registerExtensionPointGroupImpl(ExtensionContextRegisterByAnnHelper<T> helper) throws RegisterParamException, ProxyException {
        List<Object> defaultImpls = new ArrayList<>();
        List<Matcher<T>> abilities = new ArrayList<>();
        List<Matcher<T>> businesses = new ArrayList<>();

        if (!Objects.isNull(this.instanceHolders)) {
            // register by Annotation
            for (InstanceHolder holder : this.instanceHolders) {
                Object instance = holder.getInstance();
                if (instance.getClass().isAnnotationPresent(ExtensionPointDefaultImplementation.class)) {
                    defaultImpls.add(instance);
                } else if (instance.getClass().isAnnotationPresent(Ability.class)) {
                    if (instance instanceof Matcher<?> matcher) {
                        abilities.add((Matcher<T>) matcher);
                    } else {
                        throw new RegisterParamException("instance annotated with @Ability should implement Matcher interface");
                    }
                } else if (instance.getClass().isAnnotationPresent(Business.class)) {
                    if (instance instanceof Matcher<?> matcher) {
                        businesses.add((Matcher<T>) matcher);
                    } else {
                        throw new RegisterParamException("instance annotated with @Business should implement Matcher interface");
                    }
                }
            }
        }

        // register by bean
        if (!Objects.isNull(this.extensionPointGroupImplementation)) {
            defaultImpls.add(this.extensionPointGroupImplementation);
        }
        if (!Objects.isNull(this.abilities) && !this.abilities.isEmpty()) {
            abilities.addAll(this.abilities);
        }
        if (!Objects.isNull(this.businesses) && !this.businesses.isEmpty()) {
            businesses.addAll(this.businesses);
        }

        // check
        if (defaultImpls.isEmpty()) {
            throw new RegisterParamException("extension point default implementation not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist");
        }
        if (defaultImpls.size() > 1) {
            throw new RegisterParamException("More than one instance annotated with @ExtensionPointDefaultImplementation found");
        }

        //  register extension point default implementation
        helper.setExtensionPointDefaultImplementation(defaultImpls.get(0));

        // register abilities
        for (Matcher<T> ability : abilities) {
            helper.addAbilities(ability);
        }

        // register businesses
        for (Matcher<T> business : businesses) {
            helper.addBusinesses(business);
        }
    }

    @Autowired(required = false)
    public void setAbilities(List<IAbility<T>> abilities) {
        this.abilities = abilities;
    }

    @Autowired(required = false)
    public void setBusinesses(List<IBusiness<T>> businesses) {
        this.businesses = businesses;
    }

    @Autowired(required = false)
    public void setExtensionPointGroupImplementation(IExtensionPointGroupDefaultImplementation<T> extensionPointGroupImplementation) {
        this.extensionPointGroupImplementation = extensionPointGroupImplementation;
    }

    @Autowired(required = false)
    public void setExtensionPointHolders(List<ExtensionPointHolder> extensionPointHolders) {
        this.extensionPointHolders = extensionPointHolders;
    }

    @Autowired(required = false)
    private void setInstanceHolders(List<InstanceHolder> instanceHolders) {
        this.instanceHolders = instanceHolders;
    }

    @Autowired(required = false)
    public void setClassHolders(List<ClassHolder> classHolders) {
        this.classHolders = classHolders;
    }
}
