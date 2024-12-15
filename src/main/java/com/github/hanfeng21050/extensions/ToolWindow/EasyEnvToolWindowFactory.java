package com.github.hanfeng21050.extensions.ToolWindow;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.github.hanfeng21050.utils.Logger;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class EasyEnvToolWindowFactory implements ToolWindowFactory {
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建日志窗口
        EasyEnvLogWindow logWindow = new EasyEnvLogWindow(project, toolWindow);
        Content logContent = ContentFactory.getInstance().createContent(
                logWindow,
                "日志",
                false
        );
        logContent.setCloseable(false);
        toolWindow.getContentManager().addContent(logContent);
        // 设置Logger的实例和工具窗口
        Logger.setViewBarsInstance(logWindow, toolWindow);

        // 创建缓存管理窗口
        CacheMgrWindow cacheMgrWindow = new CacheMgrWindow(config);
        Content cacheContent = ContentFactory.getInstance().createContent(
                cacheMgrWindow.getPanel(),
                "缓存管理",
                false
        );
        cacheContent.setCloseable(false);
        toolWindow.getContentManager().addContent(cacheContent);
    }
}
