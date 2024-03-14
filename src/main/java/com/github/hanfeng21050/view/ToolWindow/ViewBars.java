package com.github.hanfeng21050.view.ToolWindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ViewBars extends SimpleToolWindowPanel {

    JTextArea jTextArea = new JTextArea();
    private Project project;

    public ViewBars(Project project) {
        super(false, true);
        this.project = project;

        // 设置窗体侧边栏按钮
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new SettingBar(this));
        group.add(new RefreshBar(this));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("bar", group, false);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());

        // 添加
        JBSplitter splitter = new JBSplitter(false);
        splitter.setSplitterProportionKey("main.splitter.key");
        jTextArea.setEditable(false);
        splitter.setFirstComponent(jTextArea);
        setContent(splitter);
    }

    public Project getProject() {
        return project;
    }

    static class RefreshBar extends DumbAwareAction {

        private ViewBars panel;

        public RefreshBar(ViewBars panel) {
            super("清除", "清除日志", IconLoader.getIcon("/icons/icon-delete.svg"));
            this.panel = panel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
        }

    }


    static class SettingBar extends DumbAwareAction {
        private ViewBars panel;

        public SettingBar(ViewBars panel) {
            super("配置股票", "Click to setting", IconLoader.getIcon("/META-INF/icon-gen.png"));
            this.panel = panel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
        }

    }
}


