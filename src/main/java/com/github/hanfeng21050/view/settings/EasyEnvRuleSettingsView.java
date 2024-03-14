package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfig.ConfigReplaceRule;
import com.github.hanfeng21050.utils.ObjectUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class EasyEnvRuleSettingsView extends AbstractTemplateSettingsView {
    // 私有静态变量，用于保存唯一实例
    private static EasyEnvRuleSettingsView instance;


    private List<ConfigReplaceRule> oldConfigReplaceRules;
    private List<EasyEnvConfig.ExcludedFile> oldExcludeFiles;
    private JPanel panel;
    private JPanel filterRulePanel;
    private JPanel excludedFilePanel;
    private JTable replaceRuleTable;
    private JTable excludedFileTable;
    private JBList<Map.Entry<String, String>> excludedFileMapList;
    private JBList<Map.Entry<String, ConfigReplaceRule>> configReplaceRuleMapList;
    private EasyEnvConfig config;

    private boolean isModify = false;

    private EasyEnvRuleSettingsView(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;
        try {
            this.oldConfigReplaceRules = ObjectUtil.deepCopyList(config.getConfigReplaceRules());
            this.oldExcludeFiles = ObjectUtil.deepCopyList(config.getExcludedFiles());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // 公共方法，用于获取唯一实例
    public static EasyEnvRuleSettingsView getInstance(EasyEnvConfig easyEnvConfig) {
        if (instance == null) {
            synchronized (EasyEnvRuleSettingsView.class) {
                if (instance == null) {
                    // 如果实例为空，创建一个新实例
                    instance = new EasyEnvRuleSettingsView(easyEnvConfig);
                }
            }
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
                    ConfigReplaceRule configReplaceRule = replaceRuleAddView.getEntry();
                    config.getConfigReplaceRules().add(configReplaceRule);
                    isModify = true;
                    refreshReplaceRuleTable();
                }
            }
        }).setRemoveAction(anActionButton -> {
            if (config != null) {
                int selectedRow = replaceRuleTable.getSelectedRow();
                if (selectedRow != -1) {
                    List<ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
                    configReplaceRules.remove(selectedRow);
                    isModify = true;
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
                EasyEnvConfig.ExcludedFile excludedFile = new EasyEnvConfig.ExcludedFile(UUID.randomUUID().toString(), "");
                config.getExcludedFiles().add(excludedFile);
                isModify = true;
                refreshExcludedFileTable();
            }
        }).setRemoveAction(anActionButton -> {
            if (config != null) {
                int selectedRow = excludedFileTable.getSelectedRow();
                if (selectedRow != -1) {
                    List<EasyEnvConfig.ExcludedFile> excludedFiles = config.getExcludedFiles();
                    excludedFiles.remove(selectedRow);
                    isModify = true;
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
        List<ConfigReplaceRule> configReplaceRules = config.getConfigReplaceRules();
        DefaultTableModel customModel = getReplaceRuleTableModel(configReplaceRules);
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

                    for (int i = 0; i < configReplaceRules.size(); i++) {
                        ConfigReplaceRule configReplaceRule = configReplaceRules.get(i);
                        if (configReplaceRule.getUuid().equals(key)) {
                            configReplaceRule.setFileName(fileName);
                            configReplaceRule.setReplaceStr(regExpression);
                            configReplaceRule.setReplaceStr(replaceStr);
                            isModify = true;
                        }
                    }
                }
            }
        });
    }

    public void refreshExcludedFileTable() {
        if (config != null && config.getExcludedFiles() != null) {
            List<EasyEnvConfig.ExcludedFile> excludedFiles = config.getExcludedFiles();
            DefaultTableModel customModel = getExcludedFileTableModel(excludedFiles);
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

                        // 更新数据
                        for (int i = 0; i < excludedFiles.size(); i++) {
                            EasyEnvConfig.ExcludedFile excludedFile = excludedFiles.get(i);
                            if (excludedFile.getUuid().equals(key)) {
                                excludedFile.setFileName(newValue);
                            }
                            isModify = true;
                        }
                    }
                }
            });
        }
    }

    @NotNull
    private DefaultTableModel getExcludedFileTableModel(List<EasyEnvConfig.ExcludedFile> excludedFiles) {
        Vector<Vector<String>> customData = new Vector<>(excludedFiles.size());
        for (EasyEnvConfig.ExcludedFile excludedFile : excludedFiles) {
            Vector<String> row = new Vector<>(2);
            row.add(excludedFile.getUuid());
            row.add(excludedFile.getFileName());
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers3);
    }

    @NotNull
    private DefaultTableModel getReplaceRuleTableModel(List<ConfigReplaceRule> configReplaceRules) {
        Vector<Vector<String>> customData = new Vector<>(configReplaceRules.size());

        for (ConfigReplaceRule configReplaceRule : configReplaceRules) {
            Vector<String> row = new Vector<>(4);
            row.add(configReplaceRule.getUuid());
            row.add(configReplaceRule.getFileName());
            row.add(configReplaceRule.getRegExpression());
            row.add(configReplaceRule.getReplaceStr());
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

    /**
     * 判断是否有修改。
     *
     * @return 如果有修改，返回true；否则，返回false。
     */
    public boolean isModified() {
        return isModify;
    }

    /**
     * 】
     * 应用配置
     */
    public void apply() {
        try {
            this.oldConfigReplaceRules = ObjectUtil.deepCopyList(config.getConfigReplaceRules());
            this.oldExcludeFiles = ObjectUtil.deepCopyList(config.getExcludedFiles());
            this.isModify = false;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 重置配置
     */
    public void reset() {
        try {
            this.config.setConfigReplaceRules(ObjectUtil.deepCopyList(this.oldConfigReplaceRules));
            this.config.setExcludedFiles(ObjectUtil.deepCopyList(this.oldExcludeFiles));
            refreshReplaceRuleTable();
            refreshExcludedFileTable();
            this.isModify = false;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
