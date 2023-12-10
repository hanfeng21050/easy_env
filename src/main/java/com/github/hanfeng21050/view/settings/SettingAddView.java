package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig.SeeConnectInfo;
import com.github.hanfeng21050.utils.CommonValidateUtil;
import com.github.hanfeng21050.utils.CryptUtil;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
        setTitle("添加地址");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isBlank(labelTextField.getText())) {
            return new ValidationInfo("请输入备注", labelTextField);
        }
        if (StringUtils.isBlank(addressTextField.getText())) {
            return new ValidationInfo("请输入地址", addressTextField);
        }
        if (StringUtils.isNotBlank(addressTextField.getText()) && !CommonValidateUtil.isValidURL(addressTextField.getText())) {
            return new ValidationInfo("地址输入不正确，请检查", addressTextField);
        }
        if (StringUtils.isBlank(usernameTextField.getText())) {
            return new ValidationInfo("请输入用户名", usernameTextField);
        }
        if (StringUtils.isBlank(passwordTextField.getText())) {
            return new ValidationInfo("请输入密码", passwordTextField);
        }
        return super.doValidate();
    }

    public SeeConnectInfo getEntry() {
        try {
            String uuid = UUID.randomUUID().toString();
            SeeConnectInfo seeConnectInfo = new SeeConnectInfo();
            seeConnectInfo.setUuid(uuid);
            seeConnectInfo.setLabel(labelTextField.getText());
            seeConnectInfo.setAddress(addressTextField.getText());
            seeConnectInfo.setUsername(usernameTextField.getText());
            seeConnectInfo.setPassword(CryptUtil.encryptPassword(passwordTextField.getText()));
            return seeConnectInfo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
