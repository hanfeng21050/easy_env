package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;

public class ReplaceRuleAddView extends DialogWrapper {
    private JTextField fileNameTextField;
    private JTextField regExpressionTextField;
    private JTextField replaceStrTextField;
    private JLabel fileName;
    private JLabel regExpression;
    private JLabel replaceStr;
    private JPanel panel;

    public ReplaceRuleAddView() {
        super(false);
        init();
        setTitle("添加");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return panel;
    }

    public Map.Entry<String, EasyEnvConfig.ConfigReplaceRule> getEntry() {
        try {
            String uuid = UUID.randomUUID().toString();
            EasyEnvConfig.ConfigReplaceRule configReplaceRule = new EasyEnvConfig.ConfigReplaceRule();
            configReplaceRule.setFileName(fileNameTextField.getText());
            configReplaceRule.setRegExpression(regExpressionTextField.getText());
            configReplaceRule.setReplaceStr(replaceStrTextField.getText());
            return new AbstractMap.SimpleEntry<>(uuid, configReplaceRule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isBlank(fileNameTextField.getText())) {
            return new ValidationInfo("请输入文件名", fileNameTextField);
        }
        if (StringUtils.isBlank(regExpressionTextField.getText())) {
            return new ValidationInfo("请输入正则表达式", regExpressionTextField);
        }
        return super.doValidate();
    }
}
