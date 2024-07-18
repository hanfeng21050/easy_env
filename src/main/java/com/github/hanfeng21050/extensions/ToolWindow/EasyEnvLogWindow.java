package com.github.hanfeng21050.extensions.ToolWindow;

import com.github.hanfeng21050.utils.MyIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EasyEnvLogWindow extends SimpleToolWindowPanel {

    JTextArea jTextArea = new JTextArea();
    private Project project;
    private ToolWindow toolWindow;

    public EasyEnvLogWindow(Project project, ToolWindow toolWindow) {
        super(false, true);
        this.project = project;
        this.toolWindow = toolWindow;

        // 设置窗体侧边栏按钮
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RefreshBar(this));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("bar", group, false);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(jTextArea);
        jTextArea.setEditable(false);
        JBSplitter splitter = new JBSplitter(false);
        splitter.setSplitterProportionKey("main.splitter.key");
        splitter.setFirstComponent(scrollPane);
        setContent(splitter);
    }

    public Project getProject() {
        return project;
    }

    public void appendLog(String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            jTextArea.append(message + "\n");
            jTextArea.setCaretPosition(jTextArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                jTextArea.append(message + "\n");
                jTextArea.setCaretPosition(jTextArea.getDocument().getLength());
            });
        }
    }

    static class RefreshBar extends DumbAwareAction {

        private final EasyEnvLogWindow panel;

        public RefreshBar(EasyEnvLogWindow panel) {
            super("清除", "清除日志", MyIcons.deleteIcon);
            this.panel = panel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            panel.jTextArea.setText("");
        }
    }
}
