package io.github.xiaoshicae.extension.intellij.linemarker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
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
 * 在 @ExtensionPointDefaultImplementation 类的类名旁显示 gutter icon，点击导航到扩展点接口
 */
public class DefaultImplLineMarkerProvider extends RelatedItemLineMarkerProvider {

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
        if (!PsiSearchUtil.hasAnnotation(psiClass, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION)) {
            return;
        }

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
