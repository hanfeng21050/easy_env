package com.github.hanfeng21050.extensions.inspection;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiNameValuePair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @Author hanfeng32305
 * @Date 2024/6/28 20:28
 */
public class JRESMacroVisitor extends JavaElementVisitor {

    private ProblemsHolder holder;

    public JRESMacroVisitor(ProblemsHolder holder) {
        this.holder = holder;
    }

    @Override
    public void visitAnnotation(PsiAnnotation annotation) {
        super.visitAnnotation(annotation);
        if ("com.hundsun.jres.studio.annotation.JRESMacro".equals(annotation.getQualifiedName())) {
            PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
            for (PsiNameValuePair attribute : attributes) {
                if ("value".equals(attribute.getName()) || attribute.getName() == null) {
                    PsiAnnotationMemberValue value = attribute.getValue();
                    if (value != null) {
                        String validValue = isValidValue(value.getText());
                        if (StringUtils.isNotBlank(validValue)) {
                            LocalQuickFix[] localQuickFixes = new LocalQuickFix[2];
                            localQuickFixes[0] = new NavigateToDocumentQuickFix();
                            localQuickFixes[1] = new NavigateToDocumentQuickFix2();
                            holder.registerProblem(value, validValue, ProblemHighlightType.GENERIC_ERROR, localQuickFixes);
                        }
                    }
                }
            }
        }
    }

    private String isValidValue(String value) {
        MacroSyntaxCheck macroSyntaxCheck = new MacroSyntaxCheck(value);
        return macroSyntaxCheck.syntaxCheck();
    }

    private static class NavigateToDocumentQuickFix implements LocalQuickFix {

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return "宏模板文档";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                VirtualFile[] modules = baseDir.getChildren();
                for (VirtualFile module : modules) {
                    if (module.getPath().contains("pub") && !module.getPath().contains("biz-pub")) {
                        VirtualFile virtualFile = module.findFileByRelativePath("jresProject.xml");
                        if (virtualFile != null) {
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile);
                            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                            return;
                        }
                    }
                }
            }
        }
    }

    private static class NavigateToDocumentQuickFix2 implements LocalQuickFix {

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return "恒生SQL函数";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                VirtualFile[] modules = baseDir.getChildren();
                for (VirtualFile module : modules) {
                    if (module.getPath().contains("pub") && !module.getPath().contains("biz-pub")) {
                        VirtualFile virtualFile = module.findFileByRelativePath("studio-resources/metadata/custom/HS_C_3QPETWAT.data");
                        if (virtualFile != null) {
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile);
                            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
