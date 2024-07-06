package com.github.hanfeng21050.extensions.ToolWindow;

import com.github.hanfeng21050.utils.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EasyEnvLogWindowFactory implements ToolWindowFactory {
    private static final String DISPLAY_NAME = "";

    @Override
    public boolean isApplicable(@NotNull Project project) {
        return ToolWindowFactory.super.isApplicable(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ToolWindowFactory.super.init(toolWindow);
        Project project = toolWindow.getProject();
        EasyEnvLogWindow viewPanel = new EasyEnvLogWindow(project, toolWindow);
        // 获取内容工厂的实例
        ContentFactory contentFactory = ContentFactory.getInstance();

        //创建一个Content，也就是toolwindow里面的一个tab页
        Content content = contentFactory.createContent(viewPanel, "执行日志", false);
        //将Content加入到toolwindow中
        toolWindow.getContentManager().addContent(content);

        // 设置Logger的实例和工具窗口
        Logger.setViewBarsInstance(viewPanel, toolWindow);

    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return ToolWindowFactory.super.shouldBeAvailable(project);
    }

    @Override
    public @Nullable ToolWindowAnchor getAnchor() {
        return ToolWindowFactory.super.getAnchor();
    }

    @Override
    public @Nullable Icon getIcon() {
        return ToolWindowFactory.super.getIcon();
    }
}
