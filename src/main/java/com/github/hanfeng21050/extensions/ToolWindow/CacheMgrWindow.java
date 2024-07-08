package com.github.hanfeng21050.extensions.ToolWindow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.github.hanfeng21050.utils.HttpClientUtil;
import com.intellij.openapi.application.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CacheMgrWindow {
    private final static Logger log = LoggerFactory.getLogger(CacheMgrWindow.class);

    // 登录信息
    private String auth = "";
    private Map<String, Map<String, String>> dicts = new HashMap<>();


    private final EasyEnvConfig config;
    private JComboBox<String> macroSvr;
    private JComboBox<String> nodeIp;
    private JComboBox<String> memoryTable;
    private JTable table1;
    private JButton RefresButton;
    private JPanel Jpanel;
    private JButton queryButton;
    private JTextField condition;
    private JComboBox<String> env;

    public CacheMgrWindow(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;

        // 添加按钮的事件监听器
        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performQuery();
            }
        });

        RefresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });

        // 初始化其他组件，比如JComboBox和JTable的数据
        initializeComponents();

        // 添加下拉框的事件监听器
        addComboBoxListeners();
    }

    private void initializeComponents() {
        List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = config.getSeeConnectInfos();
        if (!seeConnectInfos.isEmpty()) {
            env.addItem(null);
            for (EasyEnvConfig.SeeConnectInfo seeConnectInfo : seeConnectInfos) {
                env.addItem(seeConnectInfo.getLabel());
            }
            String selectedItem = (String) env.getSelectedItem();
            Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream().filter(x -> x.getLabel().equals(selectedItem)).findFirst();
            if (first.isPresent()) {
                EasyEnvConfig.SeeConnectInfo seeConnectInfo = first.get();
                SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                refreshMacroSvr(seeConfig);
            }
        }

        // 示例：初始化JTable
        String[] columnNames = {"Column1", "Column2"};
        Object[][] data = {
                {"Data1", "Data2"},
                {"Data3", "Data4"}
        };
        table1.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
    }


    private void addComboBoxListeners() {
        env.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                if (selectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream().filter(x -> x.getLabel().equals(selectedItem)).findFirst();
                    if (first.isPresent()) {
                        EasyEnvConfig.SeeConnectInfo seeConnectInfo = first.get();
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshMacroSvr(seeConfig);
                    }
                }
            }
        });

        macroSvr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                if (selectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream().filter(x -> x.getLabel().equals(selectedItem)).findFirst();
                    if (first.isPresent()) {
                        EasyEnvConfig.SeeConnectInfo seeConnectInfo = first.get();
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshNodeIp(seeConfig);
                    }
                }

            }
        });

        nodeIp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                if (selectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream().filter(x -> x.getLabel().equals(selectedItem)).findFirst();
                    if (first.isPresent()) {
                        EasyEnvConfig.SeeConnectInfo seeConnectInfo = first.get();
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshMemoryTable(seeConfig);
                    }
                }
            }
        });
    }

    private void performQuery() {
        // 查询逻辑
        String conditionText = condition.getText();
        // 根据conditionText执行查询操作
        System.out.println("查询条件: " + conditionText);
    }

    private void clearFields() {
        // 清空所有输入字段和选择框
        condition.setText("");
        macroSvr.setSelectedIndex(-1);
        nodeIp.setSelectedIndex(-1);
        memoryTable.setSelectedIndex(-1);
    }

    public JPanel getPanel1() {
        return Jpanel;
    }


    // 刷新微服务列表
    private void refreshMacroSvr(SeeConfig seeConfig) {
        macroSvr.removeAllItems();
        nodeIp.removeAllItems();
        memoryTable.removeAllItems();
        this.dicts = new HashMap<>();
        this.auth = "";
        // 更新 UI 必须在事件调度线程上进行
        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    SeeRequest.login(seeConfig);
                    this.auth = SeeRequest.getAuth(seeConfig);
                    return SeeRequest.getUf30AndXoneApps(seeConfig, this.auth);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).thenAccept(uf30AndXoneApps -> {

                macroSvr.removeAllItems();
                JSONArray data = uf30AndXoneApps.getJSONArray("data");

                Map<String, String> dict = new HashMap<>();
                macroSvr.addItem(null);
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    macroSvr.addItem(jsonObject.getString("name"));
                    dict.put(jsonObject.getString("name"), jsonObject.getString("id"));
                }
                this.dicts.put("macroSvr", dict);

            }).exceptionally(ex -> {
                log.error("刷新微服务失败：{}", ex.getMessage(), ex); // 异常处理
                return null;
            });
        });
    }

    // 刷新节点IP
    private void refreshNodeIp(SeeConfig seeConfig) {
        nodeIp.removeAllItems();
        memoryTable.removeAllItems();

        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                String value = dicts.get("macroSvr").get((String) macroSvr.getSelectedItem());
                if (value != null) {
                    try {
                        return SeeRequest.getLocalCacheFormDataOnlyComputer(seeConfig, this.auth, value);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps == null) return;
                JSONArray data = uf30AndXoneApps.getJSONArray("data");
                Map<String, String> dict = new HashMap<>();
                nodeIp.addItem(null);
                for (int i = 0; i < data.size(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);
                    nodeIp.addItem(jsonObject.getString("address"));
                    dict.put(jsonObject.getString("address"), jsonObject.getString("address") + ":" + jsonObject.getString("httpPort"));
                }
                this.dicts.put("nodeIp", dict);
            }).exceptionally(ex -> {
                log.error("刷新微服务失败：{}", ex.getMessage(), ex); // 异常处理
                return null;
            });
        });

    }


    // 刷新内存表
    private void refreshMemoryTable(SeeConfig seeConfig) {
        memoryTable.removeAllItems();
        // 更新 UI 必须在事件调度线程上进行
        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                String value = dicts.get("nodeIp").get((String) nodeIp.getSelectedItem());
                if (value != null) {
                    String[] split = value.split(":");
                    try {
                        return SeeRequest.getLocalCacheFormDataOnlyTable(seeConfig, this.auth, split[0], split[1]);
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps == null) return;

                JSONArray data = uf30AndXoneApps.getJSONArray("data");
                Map<String, String> dict = new HashMap<>();
                memoryTable.addItem(null);
                for (int i = 0; i < data.size(); i++) {
                    String table = data.getString(i);
                    memoryTable.addItem(table);
                    dict.put(table, table);
                }
                this.dicts.put("memoryTable", dict);
            }).exceptionally(ex -> {
                log.error("刷新微服务失败：{}", ex.getMessage(), ex); // 异常处理
                return null;
            });
        });
    }
}
