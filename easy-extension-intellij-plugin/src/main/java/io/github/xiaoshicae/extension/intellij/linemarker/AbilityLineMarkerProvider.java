package io.github.xiaoshicae.extension.intellij.linemarker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import io.github.xiaoshicae.extension.intellij.EasyExtensionIcons;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import io.github.xiaoshicae.extension.intellij.util.PsiSearchUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * 在 @Ability 类的类名旁显示 gutter icon，点击导航到实现的 @ExtensionPoint 接口
 */
public class AbilityLineMarkerProvider extends RelatedItemLineMarkerProvider {

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
        if (!PsiSearchUtil.hasAnnotation(psiClass, EasyExtensionAnnotations.ABILITY)) {
            return;
        }

        // 快速检查：直接接口中是否有 @ExtensionPoint，无需全项目搜索
        List<PsiClass> extensionPoints = PsiSearchUtil.findImplementedExtensionPoints(psiClass);
        if (extensionPoints.isEmpty()) {
            return;
        }

        String tooltip = extensionPoints.size() == 1
                ? "Extension point: " + extensionPoints.get(0).getName()
                : "Implements " + extensionPoints.size() + " extension points";

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
                .create(EasyExtensionIcons.GUTTER_INTERFACE)
                .setTargets(extensionPoints)
                .setTooltipText(tooltip);

        result.add(builder.createLineMarkerInfo(element));
    }
}
