package io.github.xiaoshicae.extension.spring.boot.autoconfigure;

import io.github.xiaoshicae.extension.core.DefaultExtensionContext;
import io.github.xiaoshicae.extension.core.ExtensionContextRegisterHelper;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.ExtensionPointHolder;
import io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.register.scanner.MatcherParamHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;


@Configuration
@EnableConfigurationProperties(EasyExtensionConfigurationProperties.class)
public class EasyExtensionAutoConfiguration<T> {
    private List<ExtensionPointHolder> extensionPointHolders;
    private MatcherParamHolder<T> matcherParamHolder;
    private IExtensionPointGroupDefaultImplementation<T> extensionPointGroupImplementation;
    private List<IBusiness<T>> businesses;
    private List<IAbility<T>> abilities;

    @Bean
    public IExtensionContext<T> registerExtensionContext(EasyExtensionConfigurationProperties properties) throws RegisterException {
        Boolean enableLog = properties.getEnableLog();
        Boolean allowUnknownBusiness = properties.getAllowUnknownBusiness();

        DefaultExtensionContext<T> manager = new DefaultExtensionContext<>(enableLog, !allowUnknownBusiness);
        ExtensionContextRegisterHelper<T> helper = new ExtensionContextRegisterHelper<>();

        if (Objects.isNull(extensionPointHolders) || extensionPointHolders.isEmpty()) {
            // empty IExtensionContext
            return manager;
        }

        for (ExtensionPointHolder holder : extensionPointHolders) {
            helper.addExtensionPointClasses(holder.getExtensionPointClass());
        }

        if (Objects.isNull(matcherParamHolder)) {
            throw new RegisterParamException("MatcherParam not found, please check instance with @MatcherParam annotation if exist");
        }
        helper.setMatcherParamClass(matcherParamHolder.getMatcherParamClass());

        helper.setExtensionPointDefaultImplementation(extensionPointGroupImplementation);
        if (Objects.isNull(extensionPointGroupImplementation)) {
            throw new RegisterParamException("extension point default implementation not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist");
        }

        if (!Objects.isNull(abilities) && !abilities.isEmpty()) {
            for (IAbility<T> ability : abilities) {
                helper.addAbilities(ability);
            }
        }

        if (!Objects.isNull(businesses) && !businesses.isEmpty()) {
            for (IBusiness<T> business : businesses) {
                helper.addBusinesses(business);
            }
        }

        helper.doRegister(manager);
        return manager;
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
    public void setMatcherParamHolder(MatcherParamHolder<T> matcherParamHolder) {
        this.matcherParamHolder = matcherParamHolder;
    }

    @Autowired(required = false)
    public void setExtensionPointHolders(List<ExtensionPointHolder> extensionPointHolders) {
        this.extensionPointHolders = extensionPointHolders;
    }
}
