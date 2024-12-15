package com.github.hanfeng21050.extensions.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.controller.EnvConfigController;
import com.github.hanfeng21050.controller.SeeRequestController;
import com.github.hanfeng21050.utils.EasyIcons;
import com.github.hanfeng21050.utils.ObjectUtil;
import com.github.hanfeng21050.utils.PasswordUtil;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.apache.commons.lang3.SerializationUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * 显示和配置EasyEnv插件设置的视图。
 */
public class EasyEnvSettingsView extends AbstractTemplateSettingsView {
    private final EasyEnvConfig config;
    private List<EasyEnvConfig.SeeConnectInfo> oldSeeConnectInfos;
    private JButton importButton;
    private JButton exportButton;
    private JPanel envPanel;
    private JPanel panel;
    private JTable envTable;
    private JBList<Map.Entry<String, EasyEnvConfig.SeeConnectInfo>> seeConnectInfoMapList;

    private boolean isModify = false;

    /**
     * 构造函数，接收EasyEnv配置并初始化视图。
     *
     * @param easyEnvConfig EasyEnv配置
     */
    public EasyEnvSettingsView(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;

        try {
            // 拷贝副本
            this.oldSeeConnectInfos = ObjectUtil.deepCopyList(config.getSeeConnectInfos());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

    /**
     * 创建自定义组件的方法。
     */
    private void createUIComponents() {
        // 初始化 importButton
        importButton = new JButton("导入配置");
        importButton.addActionListener(e -> importConfiguration());

        // 初始化 exportButton
        exportButton = new JButton("导出配置");
        exportButton.addActionListener(e -> exportConfiguration());

        // 初始化表格
        envTable = new JBTable();
        refreshEnvTable();

        envPanel = ToolbarDecorator.createDecorator(envTable)
                .setAddAction(anActionButton -> addSetting())
                .setRemoveAction(anActionButton -> removeSetting())
                .addExtraAction(createGenActionButton(this::generateConfiguration))
                .addExtraAction(createTestActionButton(this::testConnection))
                .createPanel();
    }

    /**
     * 导入配置的方法，执行与“导入配置”按钮相关的逻辑。
     */
    private void importConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("XML files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlFilter);
        fileChooser.setDialogTitle("选择配置文件");
        int result = fileChooser.showOpenDialog(panel);

        if (result == JFileChooser.APPROVE_OPTION) {
            // 用户选择了文件
            java.io.File selectedFile = fileChooser.getSelectedFile();
            // 在这里执行将 XML 文件导入到 config 的逻辑
            importConfigFromXml(selectedFile);
        }
    }

    /**
     * 从 XML 文件导入配置的方法。
     *
     * @param file 要导入的 XML 文件
     */
    private void importConfigFromXml(java.io.File file) {
        if (file.exists() && file.isFile()) {
            try {
                JAXBContext context = JAXBContext.newInstance(EasyEnvConfig.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                EasyEnvConfig importedConfig = (EasyEnvConfig) unmarshaller.unmarshal(file);

                // 处理密码
                List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = importedConfig.getSeeConnectInfos();
                for (EasyEnvConfig.SeeConnectInfo seeConnectInfo : seeConnectInfos) {
                    CredentialAttributes easyEnv = PasswordUtil.createCredentialAttributes(seeConnectInfo.getUuid());
                    Credentials credentials = new Credentials(seeConnectInfo.getUuid(), seeConnectInfo.getPassword());
                    PasswordSafe.getInstance().set(easyEnv, credentials);
                    seeConnectInfo.setPassword("");
                }
                // 在这里处理导入的配置对象
                handleImportedConfig(importedConfig);
                this.isModify = true;
                // 导入成功，显示提示信息
                Messages.showInfoMessage("导入成功", "成功");
            } catch (JAXBException e) {
                // 导入失败，显示错误信息
                Messages.showErrorDialog(e.getMessage(), "失败");
                throw new RuntimeException(e);
            }
        } else {
            // 文件不存在或不是文件
            String errorMessage = "选择的文件无效，请选择一个有效的 XML 文件。";
            Messages.showErrorDialog(errorMessage, "导入失败");
        }
    }

    /**
     * 处理导入的配置对象的方法。
     *
     * @param importedConfig 导入的配置对象
     */
    private void handleImportedConfig(EasyEnvConfig importedConfig) {
        config.setSeeConnectInfos(importedConfig.getSeeConnectInfos());
        config.setConfigReplaceRules(importedConfig.getConfigReplaceRules());
        config.setExcludedFiles(importedConfig.getExcludedFiles());
        refreshEnvTable();

        EasyEnvRuleSettingsView instance = EasyEnvRuleSettingsView.getInstance(config);
        instance.refreshReplaceRuleTable();
        instance.refreshExcludedFileTable();

        this.isModify = true;
    }

    /**
     * 导出配置的方法，执行与“导出配置”按钮相关的逻辑。
     */
    private void exportConfiguration() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择导出目录");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showSaveDialog(panel);

        if (result == JFileChooser.APPROVE_OPTION) {
            // 用户选择了目录
            java.io.File selectedDirectory = fileChooser.getSelectedFile();

            // 提示用户输入文件名，可以使用默认文件名
            String defaultFileName = "easyEnv";
            String fileName = Messages.showInputDialog(
                    "请输入文件名（包括扩展名），或按确定使用默认文件名:",
                    "输入文件名",
                    Messages.getQuestionIcon(),
                    defaultFileName,
                    null);

            if (fileName != null && !fileName.trim().isEmpty()) {
                // 用户提供了文件名，添加 .xml 扩展名
                fileName += ".xml";
                // 用户提供了文件名
                java.io.File outputFile = new java.io.File(selectedDirectory, fileName);
                exportConfigToDirectory(outputFile);
            }
        }
    }

    /**
     * 将配置导出到目录的方法。
     *
     * @param outputFile 要导出到的目录
     */
    private void exportConfigToDirectory(java.io.File outputFile) {
        if (outputFile.exists()) {
            // 文件已存在，询问用户是否覆盖
            int result = Messages.showOkCancelDialog(
                    "文件已存在，是否覆盖？",
                    "文件已存在",
                    Messages.getQuestionIcon());

            if (result != Messages.OK) {
                // 用户取消了覆盖操作
                return;
            }
        }

        JAXBContext context = null;
        try {
            context = JAXBContext.newInstance(EasyEnvConfig.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            EasyEnvConfig clone = SerializationUtils.clone(config);

            for (EasyEnvConfig.SeeConnectInfo seeConnectInfo : clone.getSeeConnectInfos()) {
                CredentialAttributes credentialAttributes = PasswordUtil.createCredentialAttributes(seeConnectInfo.getUuid());
                Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
                if (credentials != null) {
                    String password = credentials.getPasswordAsString();
                    seeConnectInfo.setPassword(password);
                }
            }
            marshaller.marshal(clone, outputFile);

            // 导出成功，显示提示信息
            String successMessage = "配置成功导出到: " + outputFile.getAbsolutePath();
            Messages.showInfoMessage(successMessage, "导出成功");
        } catch (JAXBException e) {
            // 导出失败，显示错误信息
            String errorMessage = "导出配置时发生错误: " + e.getMessage();
            Messages.showErrorDialog(errorMessage, "导出失败");
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加设置的方法，显示设置对话框并将新设置添加到配置中。
     */
    private void addSetting() {
        if (config != null) {
            SettingAddView settingAddView = new SettingAddView();
            if (settingAddView.showAndGet()) {
                EasyEnvConfig.SeeConnectInfo entry = settingAddView.getEntry();
                config.getSeeConnectInfos().add(entry);
                refreshEnvTable();
                this.isModify = true;
            }
        }
    }

    /**
     * 删除设置的方法，从配置中删除选定的设置。
     */
    private void removeSetting() {
        if (config != null) {
            int selectedRow = envTable.getSelectedRow();
            if (selectedRow != -1) {
                List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = config.getSeeConnectInfos();
                seeConnectInfos.remove(selectedRow);
                refreshEnvTable();
                this.isModify = true;
            } else {
                showInfoMessage("请选择一行");
            }
        }
    }

    /**
     * 创建带有图标的 AnActionButton 的方法。
     */
    private AnActionButton createGenActionButton(Consumer<AnActionEvent> action) {
        return new AnActionButton("生成配置", EasyIcons.genIcon) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                action.accept(e);
            }
        };
    }

    private AnActionButton createTestActionButton(Consumer<AnActionEvent> action) {
        return new AnActionButton("测试连接", EasyIcons.testIcon) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                action.accept(e);
            }
        };
    }

    /**
     * 生成配置的方法，执行与“生成配置”按钮相关的逻辑。
     */
    private void generateConfiguration(AnActionEvent e) {
        int selectedRow = envTable.getSelectedRow();
        if (selectedRow != -1) {
            // 在这里执行“测试连接”按钮的逻辑
            String uuid = (String) envTable.getValueAt(selectedRow, 0);
            String address = (String) envTable.getValueAt(selectedRow, 2);
            String username = (String) envTable.getValueAt(selectedRow, 3);
            SeeConfig seeConfig = new SeeConfig(uuid, address, username);
            Project project = e.getProject();
            EnvConfigController envConfigController = new EnvConfigController(project, seeConfig);
            envConfigController.getEnvConfig();
        } else {
            showInfoMessage("请选择一行");
        }
    }

    /**
     * 测试连接的方法，执行与“测试连接”按钮相关的逻辑。
     */
    private void testConnection(AnActionEvent e) {
        try {
            int selectedRow = envTable.getSelectedRow();
            if (selectedRow != -1) {
                // 在这里执行“测试连接”按钮的逻辑
                String uuid = (String) envTable.getValueAt(selectedRow, 0);
                String address = (String) envTable.getValueAt(selectedRow, 2);
                String username = (String) envTable.getValueAt(selectedRow, 3);
                SeeConfig seeConfig = new SeeConfig(uuid, address, username);
                SeeRequestController.login(seeConfig);
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
     * 刷新环境表格的方法。
     */
    private void refreshEnvTable() {
        List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = config.getSeeConnectInfos();

        DefaultTableModel customModel = getEnvTableModel(seeConnectInfos);
        envTable.setModel(customModel);
        envTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        envTable.getColumnModel().getColumn(0).setPreferredWidth((int) (envTable.getWidth() * 0.3));
        envTable.getColumnModel().getColumn(0).setWidth(0);
        envTable.getColumnModel().getColumn(0).setMinWidth(0);
        envTable.getColumnModel().getColumn(0).setMaxWidth(0);

        customModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                if (e.getType() == TableModelEvent.UPDATE) {
                    String key = (String) customModel.getValueAt(row, 0);
                    String label = (String) customModel.getValueAt(row, 1);
                    String address = (String) customModel.getValueAt(row, 2);
                    String username = (String) customModel.getValueAt(row, 3);

                    for (int i = 0; i < seeConnectInfos.size(); i++) {
                        EasyEnvConfig.SeeConnectInfo seeConnectInfo = seeConnectInfos.get(i);
                        if (seeConnectInfo.getUuid().equals(key)) {
                            seeConnectInfo.setLabel(label);
                            seeConnectInfo.setAddress(address);
                            seeConnectInfo.setUsername(username);
                            isModify = true;
                        }
                    }
                }
            }
        });
    }

    /**
     * 获取环境表格模型的方法。
     */
    private DefaultTableModel getEnvTableModel(List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos) {
        Vector<Vector<String>> customData = new Vector<>(seeConnectInfos.size());

        for (EasyEnvConfig.SeeConnectInfo seeConnectInfo : seeConnectInfos) {
            Vector<String> row = new Vector<>(5);
            row.add(seeConnectInfo.getUuid());
            row.add(seeConnectInfo.getLabel());
            row.add(seeConnectInfo.getAddress());
            row.add(seeConnectInfo.getUsername());
            row.add(seeConnectInfo.getPassword());
            customData.add(row);
        }
        return new DefaultTableModel(customData, headers1) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
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
     * 重置
     */
    public void reset() {
        try {
            List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = ObjectUtil.deepCopyList(oldSeeConnectInfos);
            this.config.setSeeConnectInfos(seeConnectInfos);
            refreshEnvTable();
            this.isModify = false;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存
     */
    public void apply() {
        try {
            this.oldSeeConnectInfos = ObjectUtil.deepCopyList(config.getSeeConnectInfos());
            this.isModify = false;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 显示信息提示框的方法。
     *
     * @param message 要显示的消息。
     */
    private void showInfoMessage(String message) {
        Messages.showInfoMessage(message, "提示");
    }
}
