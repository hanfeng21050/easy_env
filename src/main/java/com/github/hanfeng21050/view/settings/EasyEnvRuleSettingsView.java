package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfig.ConfigReplaceRule;
import com.google.common.collect.Maps;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.Vector;

public class EasyEnvRuleSettingsView extends AbstractTemplateSettingsView {
    // 私有静态变量，用于保存唯一实例
    private static EasyEnvRuleSettingsView instance;


    private JPanel panel;
    private JPanel filterRulePanel;
    private JPanel excludedFilePanel;
    private JTable replaceRuleTable;
    private JTable excludedFileTable;
    private JBList<Map.Entry<String, String>> excludedFileMapList;
    private JBList<Map.Entry<String, ConfigReplaceRule>> configReplaceRuleMapList;
    private EasyEnvConfig config;

    private EasyEnvRuleSettingsView(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;
    }

    // 公共方法，用于获取唯一实例
    public static EasyEnvRuleSettingsView getInstance(EasyEnvConfig easyEnvConfig) {
        if (instance == null) {
            // 如果实例为空，创建一个新实例
            instance = new EasyEnvRuleSettingsView(easyEnvConfig);
        }
        return instance;
    }


    private void createUIComponents() {
        replaceRuleTable = new JBTable();
        refreshReplaceRuleTable();
        filterRulePanel = ToolbarDecorator.createDecorator(replaceRuleTable).setAddAction(button -> {
            if (config != null) {
                ReplaceRuleAddView replaceRuleAddView = new ReplaceRuleAddView();
                if (replaceRuleAddView.showAndGet()) {
                    Map.Entry<String, EasyEnvConfig.ConfigReplaceRule> entry = replaceRuleAddView.getEntry();
                    config.getConfigReplaceRuleMap().put(entry.getKey(), entry.getValue());
                    refreshReplaceRuleTable();
                }
            }
        }).setRemoveAction(anActionButton -> {
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
        }).createPanel();


        excludedFileTable = new JBTable();
        refreshExcludedFileTable();
        excludedFilePanel = ToolbarDecorator.createDecorator(excludedFileTable).setAddAction(button -> {
            if (config != null) {
                config.getExcludedFileMap().put(UUID.randomUUID().toString(), "");
                refreshExcludedFileTable();
            }
        }).setRemoveAction(anActionButton -> {
            if (config != null) {
                int selectedRow = excludedFileTable.getSelectedRow();
                if (selectedRow != -1) {
                    SortedMap<String, String> customMap = config.getExcludedFileMap();
                    customMap.remove(excludedFileTable.getValueAt(selectedRow, 0).toString());
                    refreshExcludedFileTable();
                } else {
                    showInfoMessage("请选择一行");
                }
            }
        }).createPanel();
    }


    @Override
    public JComponent getComponent() {
        return panel;
    }


    public void refreshReplaceRuleTable() {
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

        customModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE) {
                    String key = (String) customModel.getValueAt(row, 0);
                    String fileName = (String) customModel.getValueAt(row, 1);
                    String regExpression = (String) customModel.getValueAt(row, 2);
                    String replaceStr = (String) customModel.getValueAt(row, 3);

                    ConfigReplaceRule configReplaceRule = new ConfigReplaceRule(fileName, regExpression, replaceStr);
                    config.getConfigReplaceRuleMap().put(key, configReplaceRule);
                }
            }
        });
    }

    public void refreshExcludedFileTable() {
        if (config != null && config.getExcludedFileMap() != null) {
            SortedMap<String, String> excludedFileMap = config.getExcludedFileMap();
            DefaultTableModel customModel = getExcludedFileTableModel(excludedFileMap);
            excludedFileTable.setModel(customModel);
            excludedFileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            excludedFileTable.getColumnModel().getColumn(0).setPreferredWidth((int) (replaceRuleTable.getWidth() * 0.3));
            excludedFileTable.getColumnModel().getColumn(0).setWidth(0);
            excludedFileTable.getColumnModel().getColumn(0).setMinWidth(0);
            excludedFileTable.getColumnModel().getColumn(0).setMaxWidth(0);

            customModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    int row = e.getFirstRow();
                    if (e.getType() == TableModelEvent.UPDATE) {
                        String key = (String) customModel.getValueAt(row, 0);
                        String newValue = (String) customModel.getValueAt(row, 1);
                        config.getExcludedFileMap().put(key, newValue);
                    }
                }
            });
        }
    }

    @NotNull
    private DefaultTableModel getExcludedFileTableModel(SortedMap<String, String> excludedFileMap) {
        Vector<Vector<String>> customData = new Vector<>(excludedFileMap.size());
        for (Map.Entry<String, String> entry : excludedFileMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Vector<String> row = new Vector<>(2);
            row.add(key);
            row.add(value);
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers3);
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
