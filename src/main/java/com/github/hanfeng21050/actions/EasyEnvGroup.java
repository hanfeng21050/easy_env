package com.github.hanfeng21050.actions;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.controller.EnvConfigController;
import com.github.hanfeng21050.extensions.EasyEnvConfigComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 21:53
 */
public class EasyEnvGroup extends ActionGroup {

    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    public EasyEnvGroup() {
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
        DefaultActionGroup firstGroup = new DefaultActionGroup("环境切换", true);
        firstGroup.add(new EnvChooseGroup());

        // 创建HEP+组
        DefaultActionGroup hepGroup = new DefaultActionGroup("HEP+", true);
        hepGroup.add(new HepPlusGroup.ExportInterfaceAction());

        return new AnAction[]{firstGroup, hepGroup};
    }


    private static class SubAction extends AnAction {
        private String uuid;
        private String label;
        private String address;
        private String username;
        private String password;

        public SubAction(String uuid, String label, String address, String username, String password) {
            this.uuid = uuid;
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
                SeeConfig seeConfig = new SeeConfig(uuid, address, username);
                EnvConfigController pluginLoader = new EnvConfigController(project, seeConfig);
                ApplicationManager.getApplication().invokeLater(pluginLoader::getEnvConfig);
            }

        }
    }

}
