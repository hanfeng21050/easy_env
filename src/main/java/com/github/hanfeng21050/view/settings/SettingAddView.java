package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig.*;
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
    private JTextField passwordTextField;

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

    public Map.Entry<String, CustomValue> getEntry() {
        String uuid = UUID.randomUUID().toString();
        CustomValue customValue = new CustomValue();
        customValue.setLabel(labelTextField.getText());
        customValue.setAddress(addressTextField.getText());
        customValue.setUsername(usernameTextField.getText());
        customValue.setPassword(passwordTextField.getText());

        return new SimpleEntry<>(uuid, customValue);
    }
}
