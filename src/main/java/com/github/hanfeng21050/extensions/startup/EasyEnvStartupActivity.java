package com.github.hanfeng21050.extensions.startup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class EasyEnvStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // 延迟执行，确保工具窗口已经被注册
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("EasyEnv");
            if (toolWindow != null && !toolWindow.isVisible()) {
                toolWindow.show();
            }
        }, project.getDisposed());
    }
}
