package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig.*;
import com.github.hanfeng21050.utils.CryptUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.UUID;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 10:40
 */
public class SettingAddView extends DialogWrapper {
    private JPanel panel;
    private JLabel label;
    private JLabel address;
    private JLabel username;
    private JLabel password;
    private JTextField labelTextField;
    private JTextField addressTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField;

    public SettingAddView() {
        super(false);
        init();
        setTitle("ÃÌº”µÿ÷∑");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        // todo
        return super.doValidate();
    }

    public Map.Entry<String, SeeConnectInfo> getEntry() {
        try {
            String uuid = UUID.randomUUID().toString();
            SeeConnectInfo seeConnectInfo = new SeeConnectInfo();
            seeConnectInfo.setLabel(labelTextField.getText());
            seeConnectInfo.setAddress(addressTextField.getText());
            seeConnectInfo.setUsername(usernameTextField.getText());
            seeConnectInfo.setPassword(CryptUtil.encryptPassword(passwordTextField.getText()));
            return new SimpleEntry<>(uuid, seeConnectInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
