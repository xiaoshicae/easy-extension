package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.service;


import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.AbilityInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.BusinessInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ClassInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ConfigInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.DefaultImplInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.ExtensionPointInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model.MatcherParamInfo;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.properties.EasyExtensionAdminConfigurationProperties;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.ClassUtils;
import io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util.SourceCodeReader;
import io.github.xiaoshicae.extension.core.IExtensionContext;
import io.github.xiaoshicae.extension.core.ability.IAbility;
import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.common.IProxy;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExtensionInfoService {
    private static final String[] prefixes = new String[]{"/**", "*/", "*", "//"};
    private final IExtensionContext<?> context;
    private final SourceCodeReader sourceCodeReader;
    private final EasyExtensionAdminConfigurationProperties properties;

    public ExtensionInfoService(IExtensionContext<?> context, SourceCodeReader sourceCodeReader, EasyExtensionAdminConfigurationProperties properties) {
        this.context = context;
        this.sourceCodeReader = sourceCodeReader;
        this.properties = properties;
    }

    public ConfigInfo getConfigInfo() {
        return new ConfigInfo(properties.getDocUrl());
    }

    public MatcherParamInfo getMatcherParamInfo() {
        return new MatcherParamInfo(resolveClassInfo(context.getMatcherParamClass()));
    }

    public DefaultImplInfo getDefaultImplInfo() {
        IExtensionPointGroupDefaultImplementation<?> defaultImpl = context.getExtensionPointDefaultImplementation();
        Class<?> clazz;
        if (defaultImpl instanceof IProxy<?> proxy) {
            clazz = proxy.getInstance().getClass();
        } else {
            clazz = resolveClassWithAnn(defaultImpl.getClass(), ExtensionPointDefaultImplementation.class);
        }
        return new DefaultImplInfo(resolveClassInfo(clazz));
    }

    public List<ExtensionPointInfo> getAllExtensionPoints() {
        DefaultImplInfo defaultImplInfo = getDefaultImplInfo();
        String sourceCode = defaultImplInfo.getClassInfo().getSourceCode();

        List<ExtensionPointInfo> result = new ArrayList<>();
        for (Class<?> extPointClass : context.listAllExtensionPoint()) {
            String defaultImplCode = ClassUtils.transformSourceCodeWithInterface(sourceCode, extPointClass);
            ExtensionPointInfo info = new ExtensionPointInfo(resolveClassInfo(extPointClass), defaultImplCode);
            result.add(info);
        }
        return result;
    }

    public List<AbilityInfo> getAllAbilities() {
        List<AbilityInfo> result = new ArrayList<>();
        for (IAbility<?> ability : context.listAllAbility()) {
            String code = ability.code();
            List<String> implExtensionPoints = ability.implementExtensionPoints().stream().map(Class::getName).toList();
            Class<?> clazz;
            if (ability instanceof IProxy<?> proxy) {
                clazz = proxy.getInstance().getClass();
            } else {
                clazz = resolveClassWithAnn(ability.getClass(), Ability.class);
            }
            result.add(new AbilityInfo(code, implExtensionPoints, resolveClassInfo(clazz)));
        }
        return result;
    }

    public List<BusinessInfo> getAllBusiness() {
        List<BusinessInfo> result = new ArrayList<>();
        for (IBusiness<?> business : context.listAllBusiness()) {
            String code = business.code();
            Integer priority = business.priority();
            List<BusinessInfo.UsedAbility> usedAbilities = business.usedAbilities().stream().map(e -> new BusinessInfo.UsedAbility(e.code(), e.priority())).toList();
            List<String> implementExtensionPoints = business.implementExtensionPoints().stream().map(Class::getName).toList();
            Class<?> clazz;
            if (business instanceof IProxy<?> proxy) {
                clazz = proxy.getInstance().getClass();
            } else {
                clazz = resolveClassWithAnn(business.getClass(), Business.class);
            }
            result.add(new BusinessInfo(code, priority, usedAbilities, implementExtensionPoints, resolveClassInfo(clazz)));
        }
        return result;
    }

    private ClassInfo resolveClassInfo(Class<?> clazz) {
        String name = clazz.getSimpleName();
        String fullName = clazz.getName();
        String sourceCode = sourceCodeReader.readSourceCode(clazz);
        if (Objects.isNull(sourceCode)) {
            sourceCode = ClassUtils.classInfoToString(clazz);
        }

        String[] items = ClassUtils.getClassComment(sourceCode).split("\n");
        StringBuilder builder = new StringBuilder();

        for (String item : items) {
            item = item.strip();
            for (String p : prefixes) {
                if (item.startsWith(p)) {
                    String newItem = item.substring(p.length());
                    if (!newItem.isEmpty()) {
                        builder.append(newItem.strip());
                        builder.append("\n");
                    }
                    break;
                }
            }
        }

        return new ClassInfo(name, fullName, ClassUtils.getClass(sourceCode), builder.toString());
    }

    private Class<?> resolveClassWithAnn(Class<?> clazz, Class<? extends Annotation> annotation) {
        Class<?> c = ClassUtils.resolveClassWithAnn(clazz, annotation);
        return Objects.isNull(c) ? clazz : c;
    }
}
