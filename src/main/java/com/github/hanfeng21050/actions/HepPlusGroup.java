package com.github.hanfeng21050.actions;

import com.github.hanfeng21050.dialog.FileSelectDialog;
import com.github.hanfeng21050.export.HepbizExporter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
                HepbizExporter exporter = new HepbizExporter(project);
                String openApiJson = exporter.exportToOpenAPI(project, selectedFiles);

                // 在写入动作中创建和更新文件
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        VirtualFile baseDir = project.getBaseDir();
                        VirtualFile openApiFile = baseDir.findChild("openapi.json");

                        // 在写入动作中创建文件
                        if (openApiFile == null) {
                            openApiFile = baseDir.createChildData(this, "openapi.json");
                        }

                        // 写入内容
                        openApiFile.setBinaryContent(openApiJson.getBytes(StandardCharsets.UTF_8));

                        // 保存文件路径以在写入动作后显示
                        final String filePath = openApiFile.getPath();

                        // 使用 ApplicationManager.getApplication().invokeLater 在写入动作后显示消息
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showInfoMessage(project,
                                        "OpenAPI规范已导出到: " + filePath,
                                        "导出成功")
                        );
                    } catch (IOException ex) {
                        final String errorMessage = ex.getMessage();
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showErrorDialog(project,
                                        "写入文件失败: " + errorMessage,
                                        "错误")
                        );
                    }
                });
            } catch (Exception ex) {
                Messages.showErrorDialog(project,
                        "导出失败: " + ex.getMessage(),
                        "错误");
            }
        }
    }
}
