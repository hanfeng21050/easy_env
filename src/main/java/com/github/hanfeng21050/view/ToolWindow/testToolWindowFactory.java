package com.github.hanfeng21050.view.ToolWindow;

import com.github.hanfeng21050.utils.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class testToolWindowFactory implements ToolWindowFactory {
    private static final String DISPLAY_NAME = "";

    @Override
    public boolean isApplicable(@NotNull Project project) {
        return ToolWindowFactory.super.isApplicable(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 在这里初始化工具窗口内容
        init(project, toolWindow);
    }

    public void init(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ViewBars viewPanel = new ViewBars(project, toolWindow);
        // 获取内容工厂的实例
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        // 创建一个Content，也就是toolwindow里面的一个tab页
        Content content = contentFactory.createContent(viewPanel, "执行日志", false);
        // 将Content加入到toolwindow中
        toolWindow.getContentManager().addContent(content);

        // 设置Logger的实例和工具窗口
        Logger.setViewBarsInstance(viewPanel, toolWindow);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return ToolWindowFactory.super.shouldBeAvailable(project);
    }
}
