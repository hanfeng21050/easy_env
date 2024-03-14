package com.github.hanfeng21050.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnvDeployGroup extends ActionGroup {
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{
                new SubAction("一键打包", "svrPackage"),
                new SubAction("see部署", "svrDeploy")
        };
    }

    private static class SubAction extends AnAction {
        private String label;
        private String command;

        public SubAction(String label, String command) {
            this.label = label;
            this.command = command;
            getTemplatePresentation().setText(label);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {

            System.out.println(label);
            System.out.println(command);
            Notification notification = new Notification("eastenv", "Easy Env", "功能开发中", NotificationType.INFORMATION);
            Notifications.Bus.notify(notification, e.getProject());
        }
    }
}
