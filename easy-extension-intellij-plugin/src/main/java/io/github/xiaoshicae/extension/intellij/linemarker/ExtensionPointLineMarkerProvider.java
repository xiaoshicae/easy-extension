package io.github.xiaoshicae.extension.intellij.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import io.github.xiaoshicae.extension.intellij.EasyExtensionIcons;
import io.github.xiaoshicae.extension.intellij.popup.ResolutionChainPopup;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import io.github.xiaoshicae.extension.intellij.util.PsiSearchUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * Gutter icon on @ExtensionPoint interfaces, click to show resolution chain popup.
 */
public class ExtensionPointLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            if (!(element instanceof PsiIdentifier)) {
                continue;
            }
            PsiElement parent = element.getParent();
            if (!(parent instanceof PsiClass psiClass) || !psiClass.isInterface()) {
                continue;
            }
            if (!PsiSearchUtil.hasAnnotation(psiClass, EasyExtensionAnnotations.EXTENSION_POINT)) {
                continue;
            }

            String tooltip = PsiSearchUtil.buildImplementationSummary(psiClass);

            result.add(new LineMarkerInfo<>(
                    element,
                    element.getTextRange(),
                    EasyExtensionIcons.GUTTER_IMPL,
                    e -> tooltip,
                    (e, elt) -> ResolutionChainPopup.show(psiClass, e),
                    GutterIconRenderer.Alignment.LEFT,
                    () -> "Implementations of " + psiClass.getName()
            ));
        }
    }
}
