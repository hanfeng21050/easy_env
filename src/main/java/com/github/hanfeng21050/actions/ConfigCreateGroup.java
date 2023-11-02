package com.github.hanfeng21050.actions;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.utils.MyPluginLoader;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 21:53
 */
public class ConfigCreateGroup extends ActionGroup {

    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    public ConfigCreateGroup() {
        // 设置 Action 组的显示文本
        getTemplatePresentation().setText("启动配置生成");
    }

    /**
     * Returns the child actions of the group.
     *
     * @param e
     * @see #getActionUpdateThread()
     */
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        Map<String, EasyEnvConfig.CustomValue> customMap = null;
        if (config != null) {
            customMap = config.getCustomMap();
        }
        AnAction[] actions = new AnAction[0];
        if (customMap != null) {
            actions = new AnAction[customMap.size()];
        }

        int i = 0;
        if (customMap != null) {
            for (String s : customMap.keySet()) {
                EasyEnvConfig.CustomValue customValue = customMap.get(s);
                actions[i] = new SubAction(customValue.getLabel(), customValue.getAddress(), customValue.getUsername(), customValue.getPassword());
                i++;
            }
        }
        return actions;
    }


    private static class SubAction extends AnAction {
        private String label;
        private String address;
        private String username;
        private String password;

        public SubAction(String label, String address, String username, String password) {
            this.label = label;
            this.address = address;
            this.username = username;
            this.password = password;
            getTemplatePresentation().setText(label);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            Project project = e.getProject();
            if (project != null) {
                SeeConfig seeConfig = new SeeConfig(address, username, password);
                MyPluginLoader pluginLoader = new MyPluginLoader(project, seeConfig);
                ApplicationManager.getApplication().invokeLater(pluginLoader::startBlockingLoadingProcess);
            }

        }
    }

}
