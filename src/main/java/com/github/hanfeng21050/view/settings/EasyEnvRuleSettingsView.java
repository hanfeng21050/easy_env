package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfig.ConfigReplaceRule;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.google.common.collect.Maps;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.Vector;

public class EasyEnvRuleSettingsView extends AbstractTemplateSettingsView {
    private JPanel panel;
    private JPanel filterRulePanel;
    private JPanel excludedFilePanel;
    private JTable replaceRuleTable;
    private JTable excludedFileTable;
    private JBList<Map.Entry<String, String>> excludedFileMapList;
    private JBList<Map.Entry<String, ConfigReplaceRule>> configReplaceRuleMapList;
    private EasyEnvConfig config;

    public EasyEnvRuleSettingsView(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;
    }

    private void createUIComponents() {
        replaceRuleTable = new JBTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        refreshReplaceRuleTable();
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(replaceRuleTable);
        toolbarDecorator.setAddAction(button -> {
            if (config != null) {
                ReplaceRuleAddView replaceRuleAddView = new ReplaceRuleAddView();
                if (replaceRuleAddView.showAndGet()) {
                    Map.Entry<String, EasyEnvConfig.ConfigReplaceRule> entry = replaceRuleAddView.getEntry();
                    config.getConfigReplaceRuleMap().put(entry.getKey(), entry.getValue());
                    refreshReplaceRuleTable();
                }
            }
        });
        toolbarDecorator.setRemoveAction(anActionButton -> {
            if (config != null) {
                int selectedRow = replaceRuleTable.getSelectedRow();
                if (selectedRow != -1) {
                    Map<String, EasyEnvConfig.ConfigReplaceRule> customMap = config.getConfigReplaceRuleMap();
                    customMap.remove(replaceRuleTable.getValueAt(selectedRow, 0).toString());
                    refreshReplaceRuleTable();
                } else {
                    showInfoMessage("请选择一行");
                }
            }
        });
        filterRulePanel = toolbarDecorator.createPanel();
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }


    private void refreshReplaceRuleTable() {
        Map<String, EasyEnvConfig.ConfigReplaceRule> customMap = Maps.newHashMap();
        if (config != null && config.getConfigReplaceRuleMap() != null) {
            customMap = config.getConfigReplaceRuleMap();
        }
        DefaultTableModel customModel = getReplaceRuleTableModel(customMap);
        replaceRuleTable.setModel(customModel);
        replaceRuleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        replaceRuleTable.getColumnModel().getColumn(0).setPreferredWidth((int) (replaceRuleTable.getWidth() * 0.3));
        replaceRuleTable.getColumnModel().getColumn(0).setWidth(0);
        replaceRuleTable.getColumnModel().getColumn(0).setMinWidth(0);
        replaceRuleTable.getColumnModel().getColumn(0).setMaxWidth(0);
    }

    @NotNull
    private DefaultTableModel getReplaceRuleTableModel(Map<String, EasyEnvConfig.ConfigReplaceRule> customMap) {
        Vector<Vector<String>> customData = new Vector<>(customMap.size());
        for (Map.Entry<String, EasyEnvConfig.ConfigReplaceRule> entry : customMap.entrySet()) {
            String key = entry.getKey();
            EasyEnvConfig.ConfigReplaceRule value = entry.getValue();
            Vector<String> row = new Vector<>(4);
            row.add(key);
            row.add(value.getFileName());
            row.add(value.getRegExpression());
            row.add(value.getReplaceStr());
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers2);
    }

    /**
     * 显示信息提示框的方法。
     */
    private void showInfoMessage(String message) {
        Messages.showInfoMessage(message, "提示");
    }
}
