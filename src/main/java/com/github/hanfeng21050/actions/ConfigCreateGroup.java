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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = null;
        if (config != null) {
            seeConnectInfos = config.getSeeConnectInfos();
        }
        AnAction[] actions = new AnAction[0];
        if (seeConnectInfos != null) {
            actions = new AnAction[seeConnectInfos.size()];
        }

        if (seeConnectInfos != null) {
            for (int i = 0; i < seeConnectInfos.size(); i++) {
                EasyEnvConfig.SeeConnectInfo seeConnectInfo = seeConnectInfos.get(i);
                actions[i] = new SubAction(seeConnectInfo.getLabel(), seeConnectInfo.getAddress(), seeConnectInfo.getUsername(), seeConnectInfo.getPassword());
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
