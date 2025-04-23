package com.github.hanfeng21050.actions;

import com.github.hanfeng21050.controller.export.HepExporter;
import com.github.hanfeng21050.controller.export.OpenApiExporterController;
import com.github.hanfeng21050.dialog.export.FileSelectDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * HEP+ 功能组
 */
public class HepPlusGroup {
    /**
     * 接口导出动作
     */
    public static class ExportInterfaceAction extends AnAction {
        public ExportInterfaceAction() {
            super("接口导出");
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getProject();
            if (project == null) return;

            // 使用 FilenameIndex 查找所有 .hepbiz 文件
            Collection<VirtualFile> hepbizFiles = FilenameIndex.getAllFilesByExt(project, "hepbiz", GlobalSearchScope.projectScope(project));

            if (hepbizFiles.isEmpty()) {
                Messages.showInfoMessage(project, "未找到 .hepbiz 文件", "HEP+");
                return;
            }

            // 显示文件选择对话框
            FileSelectDialog dialog = new FileSelectDialog(project, hepbizFiles);
            if (!dialog.showAndGet()) {
                return;
            }

            // 获取选中的文件
            List<VirtualFile> selectedFiles = dialog.getSelectedFiles();
            if (selectedFiles.isEmpty()) {
                Messages.showWarningDialog(project, "请至少选择一个文件", "警告");
                return;
            }

            try {
                // 创建导出器并执行导出
                HepExporter hepExporter = new OpenApiExporterController(project);
                String openApiJson = hepExporter.export(project, selectedFiles);
                if (openApiJson == null) {
                    // 用户取消了导出操作
                    return;
                }
            } catch (Exception ex) {
                Messages.showErrorDialog(project,
                        "导出失败: " + ex.getMessage(),
                        "错误");
            }
        }
    }
}
