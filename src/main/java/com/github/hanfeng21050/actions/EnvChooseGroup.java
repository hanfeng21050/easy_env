package com.github.hanfeng21050.actions;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.controller.EnvConfigController;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 21:53
 */
public class EnvChooseGroup extends ActionGroup {

    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    public EnvChooseGroup() {
        // 设置 Action 组的显示文本
        getTemplatePresentation().setText("环境配置");
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
                actions[i] = new SubAction(seeConnectInfo.getUuid(), seeConnectInfo.getLabel(), seeConnectInfo.getAddress(), seeConnectInfo.getUsername());
            }
        }
        return actions;
    }


    private static class SubAction extends AnAction {
        private String uuid;
        private String label;
        private String address;
        private String username;

        public SubAction(String uuid, String label, String address, String username) {
            this.uuid = uuid;
            this.label = label;
            this.address = address;
            this.username = username;
            getTemplatePresentation().setText(label);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            Project project = e.getProject();
            if (project != null) {
                SeeConfig seeConfig = new SeeConfig(uuid, address, username);
                EnvConfigController pluginLoader = new EnvConfigController(project, seeConfig);

                // 使用后台任务替代UI线程
                Task.Backgroundable task = new Task.Backgroundable(project, "加载环境配置...", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        pluginLoader.getEnvConfig();
                    }
                };
                task.queue();
            }

        }
    }

}
