package io.github.xiaoshicae.extension.intellij;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import org.jetbrains.annotations.NotNull;

/**
 * Tells IntelliJ that @ExtensionInject fields are implicitly written by the framework at runtime,
 * preventing them from being grayed out as "unused" or "never assigned".
 */
public class ExtensionInjectImplicitUsageProvider implements ImplicitUsageProvider {

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        return isExtensionInjectField(element);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return isExtensionInjectField(element);
    }

    private boolean isExtensionInjectField(PsiElement element) {
        return element instanceof PsiField field
                && field.getAnnotation(EasyExtensionAnnotations.EXTENSION_INJECT) != null;
    }
}
