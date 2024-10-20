package com.github.hanfeng21050.extensions.ToolWindow;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class CacheMgrWindowFactory implements ToolWindowFactory {
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        ToolWindowFactory.super.init(toolWindow);
        CacheMgrWindow cacheMgrWindow = new CacheMgrWindow(config);
        // 获取内容工厂的实例
        ContentFactory contentFactory = ContentFactory.getInstance();

        //创建一个Content，也就是toolwindow里面的一个tab页
        Content content = contentFactory.createContent(cacheMgrWindow.getPanel(), "服务器缓存", false);

        //将Content加入到toolwindow中
        toolWindow.getContentManager().addContent(content);
    }
}
