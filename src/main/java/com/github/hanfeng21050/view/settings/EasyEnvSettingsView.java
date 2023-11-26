package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.EasyEnvConfigComponent;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.github.hanfeng21050.utils.MyPluginLoader;
import com.google.common.collect.Maps;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * CommonSettingsView 提供了一个通用设置视图，包括添加、删除、生成配置和测试连接等操作。
 *
 * @Author hanfeng32305
 * @Date 2023/10/30 17:24
 */
public class EasyEnvSettingsView extends AbstractTemplateSettingsView {
    private final JPanel panel;
    private final EasyEnvConfig config = ServiceManager.getService(EasyEnvConfigComponent.class).getState();

    private final JTable customTable;

    private boolean isModify = false;

    /**
     * CommonSettingsView 构造函数，初始化视图和相关操作。
     */
    public EasyEnvSettingsView() {
        super();
        customTable = new JBTable();
        refreshCustomTable();

        panel = ToolbarDecorator.createDecorator(customTable)
                .setAddAction(anActionButton -> addSetting())
                .setRemoveAction(anActionButton -> removeSetting())
                .addExtraAction(createActionButton("生成配置", "/META-INF/icon-gen.png", this::generateConfiguration))
                .addExtraAction(createActionButton("测试连接", "/META-INF/icon-test.png", this::testConnection))
                .createPanel();
    }

    /**
     * 添加设置的方法，显示设置对话框并将新设置添加到配置中。
     */
    private void addSetting() {
        if (config != null) {
            SettingAddView settingAddView = new SettingAddView();
            if (settingAddView.showAndGet()) {
                Map.Entry<String, EasyEnvConfig.SeeConnectInfo> entry = settingAddView.getEntry();
                config.getSeeConnectInfoMap().put(entry.getKey(), entry.getValue());
                refreshCustomTable();
                this.isModify = true;
            }
        }
    }

    /**
     * 删除设置的方法，从配置中删除选定的设置。
     */
    private void removeSetting() {
        if (config != null) {
            int selectedRow = customTable.getSelectedRow();
            if (selectedRow != -1) {
                Map<String, EasyEnvConfig.SeeConnectInfo> customMap = config.getSeeConnectInfoMap();
                customMap.remove(customTable.getValueAt(selectedRow, 0).toString());
                refreshCustomTable();
                this.isModify = true;
            } else {
                showInfoMessage("请选择一行");
            }
        }
    }

    /**
     * 生成配置的方法，执行与“生成配置”按钮相关的逻辑。
     */
    private void generateConfiguration(AnActionEvent e) {
        int selectedRow = customTable.getSelectedRow();
        if (selectedRow != -1) {
            // 在这里执行“测试连接”按钮的逻辑
            String address = (String) customTable.getValueAt(selectedRow, 2);
            String username = (String) customTable.getValueAt(selectedRow, 3);
            String password = (String) customTable.getValueAt(selectedRow, 4);

            SeeConfig seeConfig = new SeeConfig(address, username, password);
            Project project = e.getProject();
            MyPluginLoader myPluginLoader = new MyPluginLoader(project, seeConfig);
            myPluginLoader.startBlockingLoadingProcess();
        } else {
            showInfoMessage("请选择一行");
        }
    }

    /**
     * 测试连接的方法，执行与“测试连接”按钮相关的逻辑。
     */
    private void testConnection(AnActionEvent e) {
        try {
            int selectedRow = customTable.getSelectedRow();
            if (selectedRow != -1) {
                // 在这里执行“测试连接”按钮的逻辑
                String address = (String) customTable.getValueAt(selectedRow, 2);
                String username = (String) customTable.getValueAt(selectedRow, 3);
                String password = (String) customTable.getValueAt(selectedRow, 4);

                SeeConfig seeConfig = new SeeConfig(address, username, password);
                SeeRequest.login(seeConfig);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showInfoMessage("连接成功", "提示");
                });
            } else {
                showInfoMessage("请选择一行");
            }
        } catch (Exception ex) {
            String errMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog("连接失败，请检查。" + errMsg, "错误");
            });
            throw new RuntimeException(ex);
        }

    }

    /**
     * 显示信息提示框的方法。
     */
    private void showInfoMessage(String message) {
        Messages.showInfoMessage(message, "提示");
    }

    /**
     * 创建带有图标的 AnActionButton 的方法。
     */
    private AnActionButton createActionButton(String text, String iconPath, Consumer<AnActionEvent> action) {
        return new AnActionButton(text, IconLoader.getIcon(iconPath, Objects.requireNonNull(ReflectionUtil.getGrandCallerClass()))) {

            @Override
            public void actionPerformed(AnActionEvent e) {
                action.accept(e);
            }
        };
    }

    /**
     * 获取视图组件的方法。
     *
     * @return 视图组件
     */
    public JComponent getComponent() {
        return panel;
    }

    /**
     * 刷新视图的方法。
     */
    public void refresh() {
        // 可以在这里添加刷新逻辑
    }

    /**
     * 刷新表格数据的方法，从配置中读取自定义变量并更新表格。
     */
    private void refreshCustomTable() {
        Map<String, EasyEnvConfig.SeeConnectInfo> customMap = Maps.newHashMap();
        if (config != null && config.getSeeConnectInfoMap() != null) {
            customMap = config.getSeeConnectInfoMap();
        }
        DefaultTableModel customModel = getDefaultTableModel(customMap);
        customTable.setModel(customModel);
        customTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customTable.getColumnModel().getColumn(0).setPreferredWidth((int) (customTable.getWidth() * 0.3));
        customTable.getColumnModel().getColumn(0).setWidth(0);
        customTable.getColumnModel().getColumn(0).setMinWidth(0);
        customTable.getColumnModel().getColumn(0).setMaxWidth(0);

        customModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE) {
                    String key = (String) customModel.getValueAt(row, 0);
                    String label = (String) customModel.getValueAt(row, 1);
                    String address = (String) customModel.getValueAt(row, 2);
                    String username = (String) customModel.getValueAt(row, 3);
                    String password = (String) customModel.getValueAt(row, 4);

                    EasyEnvConfig.SeeConnectInfo seeConnectInfo = new EasyEnvConfig.SeeConnectInfo(label, address, username, password);
                    if (config != null) {
                        config.getSeeConnectInfoMap().put(key, seeConnectInfo);
                    }
                }
            }
        });
    }

    /**
     * 从配置中读取数据的方法，生成默认表格模型。
     *
     * @param customMap 自定义变量映射
     * @return 表格模型
     */
    @NotNull
    private DefaultTableModel getDefaultTableModel(Map<String, EasyEnvConfig.SeeConnectInfo> customMap) {
        Vector<Vector<String>> customData = new Vector<>(customMap.size());
        for (Map.Entry<String, EasyEnvConfig.SeeConnectInfo> entry : customMap.entrySet()) {
            String key = entry.getKey();
            EasyEnvConfig.SeeConnectInfo value = entry.getValue();
            Vector<String> row = new Vector<>(5);
            row.add(key);
            row.add(value.getLabel());
            row.add(value.getAddress());
            row.add(value.getUsername());
            row.add(value.getPassword());
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers1);
    }

    /**
     * 判断修改状态
     * @return
     */
    public boolean isModified() {
        return isModify;
    }
}