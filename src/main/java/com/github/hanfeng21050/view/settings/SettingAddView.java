package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig.*;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 10:40
 */
public class SettingAddView extends DialogWrapper {
    private JPanel panel;
    private JLabel address;
    private JLabel username;
    private JLabel password;
    private JTextField addressTextField;
    private JTextField usernameTextField;
    private JTextField passwordTextField;

    public SettingAddView() {
        super(false);
        init();
        setTitle("添加地址");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (addressTextField.getText() == null || usernameTextField.getText().isEmpty()) {
            return new ValidationInfo("请输入正确的地址", addressTextField);
        }
        if (usernameTextField.getText() == null || usernameTextField.getText().isEmpty()) {
            return new ValidationInfo("请输入正确的用户名", usernameTextField);
        }
        if (passwordTextField.getText() == null || passwordTextField.getText().isEmpty()) {
            return new ValidationInfo("请输入正确的密码", passwordTextField);
        }
        return super.doValidate();
    }

    public Map.Entry<String, CustomValue> getEntry() {
        return new SimpleEntry<>(addressTextField.getText(),
                new CustomValue(usernameTextField.getText(), passwordTextField.getText()));
    }
}
