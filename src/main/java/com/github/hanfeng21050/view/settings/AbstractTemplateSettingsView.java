package com.github.hanfeng21050.view.settings;

import com.github.hanfeng21050.config.EasyEnvConfig;

import javax.swing.*;
import java.util.Vector;

/**
 * @Author hanfeng32305
 * @Date 2023/10/31 9:49
 */
public abstract class AbstractTemplateSettingsView {
    protected static Vector<String> headers1;
    protected static Vector<String> headers2;
    protected static Vector<String> headers3;

    static {
        headers1 = new Vector<>(5);
        headers1.add("uuid");
        headers1.add("名称");
        headers1.add("地址");
        headers1.add("用户名");
        headers1.add("密码");

        headers2 = new Vector<>(4);
        headers2.add("uuid");
        headers2.add("文件名");
        headers2.add("正则表达式");
        headers2.add("替换文本 ");

        headers3 = new Vector<>(2);
        headers3.add("uuid");
        headers3.add("文件名");

    }

    protected EasyEnvConfig config;

    public AbstractTemplateSettingsView() {
    }

    public AbstractTemplateSettingsView(EasyEnvConfig config) {
        this.config = config;
    }

    public abstract JComponent getComponent();
}
