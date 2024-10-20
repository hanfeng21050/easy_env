package com.github.hanfeng21050.extensions.lineMarker;

import com.github.hanfeng21050.utils.MyIcons;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

public class JRESMarcoLineMarker implements LineMarkerProvider {
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        LineMarkerInfo lineMarkerInfo = null;

        String anno = "com.hundsun.jres.studio.annotation.JRESMacro";
        if (!judgeHaveAnnotation(psiElement, anno)) {
            return lineMarkerInfo;
        }

        PsiMethod field = ((PsiMethod) psiElement);
        PsiAnnotation psiAnnotation = field.getAnnotation(anno);
        lineMarkerInfo = new LineMarkerInfo<>(psiAnnotation, psiAnnotation.getTextRange(), MyIcons.mainIcon,
                createTooltipProvider(),
                new IconNavigationHandler(),
                GutterIconRenderer.Alignment.LEFT);

        return lineMarkerInfo;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        LineMarkerProvider.super.collectSlowLineMarkers(elements, result);
    }

    private boolean judgeHaveAnnotation(@NotNull PsiElement psiElement, String anno) {
        return psiElement instanceof PsiMethod method && method.getAnnotation(anno) != null;

    }

    private Function<PsiElement, String> createTooltipProvider() {
        return psiElement -> "Tooltip for ";
    }

    static class IconNavigationHandler implements GutterIconNavigationHandler {

        @Override
        public void navigate(MouseEvent mouseEvent, PsiElement psiElement) {
            JRESMaroParser dialog = new JRESMaroParser();
            dialog.pack();
            dialog.setVisible(true);
            System.exit(0);
        }
    }
}