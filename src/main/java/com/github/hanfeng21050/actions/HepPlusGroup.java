package com.github.hanfeng21050.actions;

import com.alibaba.fastjson2.JSON;
import com.github.hanfeng21050.controller.export.HepExporter;
import com.github.hanfeng21050.controller.export.OpenApiExporterController;
import com.github.hanfeng21050.dialog.MenuTreeDialog;
import com.github.hanfeng21050.dialog.export.FileSelectDialog;
import com.github.hanfeng21050.model.MenuFunctionData;
import com.github.hanfeng21050.utils.Logger;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
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

    public static class SqlDealAction extends AnAction {
        public SqlDealAction() {super("oracle菜单功能脚本");}

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getProject();
            if (project == null) return;

            try {
                // 查找 menuFunction.data 文件
                VirtualFile menuDataFile = findMenuFunctionDataFile(project);
                if (menuDataFile == null) {
                    Messages.showInfoMessage(project, 
                        "未找到 menuFunction.data 文件\n预期路径：xxx-pub/studio-resources/metadata/menuFunction.data",
                        "Oracle菜单功能脚本");
                    return;
                }

                Logger.info("找到菜单数据文件: " + menuDataFile.getPath());

                // 读取并解析JSON文件
                MenuFunctionData menuData = parseMenuDataFile(menuDataFile);
                if (menuData == null) {
                    Messages.showErrorDialog(project, "解析菜单数据文件失败", "错误");
                    return;
                }

                // 显示菜单树对话框
                MenuTreeDialog dialog = new MenuTreeDialog(project, menuData);
                if (dialog.showAndGet()) {
                    // 用户点击了确定，获取选中的项目
                    List<MenuTreeDialog.MenuTreeNodeData> selectedItems = dialog.getSelectedItems();
                    if (selectedItems.isEmpty()) {
                        Messages.showInfoMessage(project, "未选择任何项目", "提示");
                        return;
                    }

                    // 显示选中的项目信息
                    showSelectedItems(project, selectedItems);
                }
                
            } catch (Exception ex) {
                Logger.error("处理菜单功能失败: " + ex.getMessage(), ex);
                Messages.showErrorDialog(project,
                        "处理失败: " + ex.getMessage(),
                        "错误");
            }
        }

        /**
         * 查找 menuFunction.data 文件
         */
        private VirtualFile findMenuFunctionDataFile(Project project) {
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir == null) {
                return null;
            }

            // 搜索所有 xxx-pub 目录
            VirtualFile[] children = baseDir.getChildren();
            for (VirtualFile child : children) {
                if (child.isDirectory() && child.getName().endsWith("-pub")) {
                    // 查找 studio-resources/metadata/menu 目录
                    VirtualFile studioResources = child.findChild("studio-resources");
                    if (studioResources != null && studioResources.isDirectory()) {
                        VirtualFile metadata = studioResources.findChild("metadata");
                        if (metadata != null && metadata.isDirectory()) {
                            VirtualFile menuFunctionFile = metadata.findChild("menuFunction.data");
                            if (menuFunctionFile != null && !menuFunctionFile.isDirectory()) {
                                return menuFunctionFile;
                            }
                        }
                    }
                }
            }
            return null;
        }

        /**
         * 解析菜单数据文件
         */
        private MenuFunctionData parseMenuDataFile(VirtualFile file) {
            try {
                String content = new String(file.contentsToByteArray(), "UTF-8");
                return JSON.parseObject(content, MenuFunctionData.class);
            } catch (Exception ex) {
                Logger.error("解析菜单数据文件失败: " + ex.getMessage(), ex);
                return null;
            }
        }

        /**
         * 显示选中的项目信息
         */
        private void showSelectedItems(Project project, List<MenuTreeDialog.MenuTreeNodeData> selectedItems) {
            StringBuilder message = new StringBuilder("选中的项目 (");
            message.append(selectedItems.size()).append(" 项):\n\n");

            int menuCount = 0;
            int functionCount = 0;

            for (MenuTreeDialog.MenuTreeNodeData item : selectedItems) {
                if (item.isMenu()) {
                    menuCount++;
                    message.append("📁 菜单: ").append(item.toString()).append("\n");
                } else {
                    functionCount++;
                    message.append("⚙️ 功能: ").append(item.toString()).append("\n");
                }
            }

            message.append("\n统计: 菜单 ").append(menuCount).append(" 个，功能号 ").append(functionCount).append(" 个");

            Messages.showInfoMessage(project, message.toString(), "选中项目");
        }
    }
}
