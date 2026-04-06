package io.github.xiaoshicae.extension.intellij.linemarker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import io.github.xiaoshicae.extension.intellij.EasyExtensionIcons;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import io.github.xiaoshicae.extension.intellij.util.PsiSearchUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 在 @Business 类的类名旁显示 gutter icon，点击导航到实现的扩展点接口和使用的 Ability 类
 */
public class BusinessLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element,
                                            @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiIdentifier)) {
            return;
        }
        PsiElement parent = element.getParent();
        if (!(parent instanceof PsiClass psiClass) || psiClass.isInterface()) {
            return;
        }
        if (!PsiSearchUtil.hasAnnotation(psiClass, EasyExtensionAnnotations.BUSINESS)) {
            return;
        }

        // 延迟加载导航目标
        NotNullLazyValue<Collection<? extends PsiElement>> targets = NotNullLazyValue.lazy(() -> {
            List<PsiElement> list = new ArrayList<>();

            // 实现的扩展点接口
            list.addAll(PsiSearchUtil.findImplementedExtensionPoints(psiClass));

            // 使用的 Ability 类
            PsiAnnotation businessAnn = psiClass.getAnnotation(EasyExtensionAnnotations.BUSINESS);
            if (businessAnn != null) {
                for (String code : PsiSearchUtil.parseAbilityCodes(businessAnn)) {
                    PsiClass abilityClass = PsiSearchUtil.findAbilityByCode(psiClass.getProject(), code);
                    if (abilityClass != null) {
                        list.add(abilityClass);
                    }
                }
            }
            return list;
        });

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
                .create(EasyExtensionIcons.GUTTER_INTERFACE)
                .setTargets(targets)
                .setTooltipText("Navigate to extension point interfaces and abilities")
                .setPopupTitle("Relations of " + psiClass.getName());

        result.add(builder.createLineMarkerInfo(element));
    }
}
