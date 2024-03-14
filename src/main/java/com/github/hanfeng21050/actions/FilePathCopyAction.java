package com.github.hanfeng21050.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilePathCopyAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // 获取当前选中的文件列表
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        if (selectedFiles != null && selectedFiles.length > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            VirtualFile baseDir = project.getBaseDir();
            String basePath = baseDir.getPath();

            // 遍历选中的文件列表并输出文件路径
            for (VirtualFile file : selectedFiles) {
                String filePath = file.getPath();
                String relativePath = getRelativePath(basePath, filePath);

                stringBuilder.append(relativePath).append("\n");
            }
            // 创建StringSelection对象，用于包装要复制的字符串
            StringSelection selection = new StringSelection(stringBuilder.toString());

            // 获取系统剪切板
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                // 将StringSelection对象放入剪切板
                clipboard.setContents(selection, null);

                // 通知用户复制操作已完成
                Notification notification = new Notification("eastenv", "Easy Env", "文件路径已复制", NotificationType.INFORMATION);
                Notifications.Bus.notify(notification, e.getProject());
            } catch (HeadlessException ex) {
                // 处理无法获取系统剪切板的情况
                System.err.println("无法获取系统剪切板：" + ex.getMessage());
            } catch (Exception ex) {
                // 处理其他异常情况
                ex.printStackTrace();
            }
        }
    }

    private String getRelativePath(String basePath, String filePath) {
        Path basePathPath = Paths.get(basePath);
        Path filePathPath = Paths.get(filePath);
        return basePathPath.relativize(filePathPath).toString();
    }
}
