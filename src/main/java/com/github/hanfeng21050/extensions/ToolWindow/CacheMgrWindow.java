package com.github.hanfeng21050.extensions.ToolWindow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.intellij.openapi.application.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CacheMgrWindow {
    private static final Logger log = LoggerFactory.getLogger(CacheMgrWindow.class);

    // 登录信息
    private String auth = "";
    private Map<String, Map<String, String>> dicts = new HashMap<>();
    private final EasyEnvConfig config;
    private JComboBox<String> macroSvr;
    private JComboBox<String> nodeIp;
    private JComboBox<String> memoryTable;
    private JTable table1;
    private JButton refreshButton;
    private JPanel panel;
    private JButton queryButton;
    private JTextField condition;
    private JComboBox<String> env;

    private Model model;

    public CacheMgrWindow(EasyEnvConfig easyEnvConfig) {
        this.config = easyEnvConfig;

        // 添加按钮的事件监听器
        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    performQuery();
                } catch (IOException | URISyntaxException ex) {
                    handleError(ex);
                }
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                env.removeAllItems();
                macroSvr.removeAllItems();
                nodeIp.removeAllItems();
                memoryTable.removeAllItems();
                condition.setText("");
                table1.setModel(new javax.swing.table.DefaultTableModel(new String[0][0], new String[0]));

                initializeComponents();
            }
        });

        // 初始化组件
        initializeComponents();

        // 添加下拉框的事件监听器
        addComboBoxListeners();
    }

    private void initializeComponents() {
        model = new Model();
        List<EasyEnvConfig.SeeConnectInfo> seeConnectInfos = config.getSeeConnectInfos();
        if (!seeConnectInfos.isEmpty()) {
            env.removeAllItems();
            for (EasyEnvConfig.SeeConnectInfo seeConnectInfo : seeConnectInfos) {
                env.addItem(seeConnectInfo.getLabel());
            }
            model.setEnv((String) env.getSelectedItem());

            String selectedItem = (String) env.getSelectedItem();
            if (selectedItem != null) {
                Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream()
                        .filter(x -> x.getLabel().equals(selectedItem))
                        .findFirst();
                first.ifPresent(seeConnectInfo -> {
                    SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                    refreshMacroSvr(seeConfig);
                });
            }
        }
    }

    private void addComboBoxListeners() {
        env.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                if (selectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream()
                            .filter(x -> x.getLabel().equals(selectedItem))
                            .findFirst();
                    first.ifPresent(seeConnectInfo -> {
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshMacroSvr(seeConfig);
                    });
                }
            }
        });

        macroSvr.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                String macroSvrSelectedItem = (String) macroSvr.getSelectedItem();
                model.setMacroName(macroSvrSelectedItem);

                if (macroSvrSelectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream()
                            .filter(x -> x.getLabel().equals(selectedItem))
                            .findFirst();
                    first.ifPresent(seeConnectInfo -> {
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshNodeIp(seeConfig);
                    });
                }
            }
        });

        nodeIp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                String nodeIpSelectedItem = (String) nodeIp.getSelectedItem();

                if (nodeIpSelectedItem != null) {
                    String[] split = dicts.get("nodeIp").get(nodeIpSelectedItem).split(":");
                    model.setNodeIp(new NodeIp(split[0], split[1]));
                }

                if (selectedItem != null) {
                    Optional<EasyEnvConfig.SeeConnectInfo> first = config.getSeeConnectInfos().stream()
                            .filter(x -> x.getLabel().equals(selectedItem))
                            .findFirst();
                    first.ifPresent(seeConnectInfo -> {
                        SeeConfig seeConfig = new SeeConfig(seeConnectInfo);
                        refreshMemoryTable(seeConfig);
                    });
                }
            }
        });

        memoryTable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) memoryTable.getSelectedItem();
                model.setMemoryTable(selectedItem);
            }
        });
    }

    private void performQuery() throws IOException, URISyntaxException {
        ApplicationManager.getApplication().invokeLater(() -> {
            Map<String, String> params = new HashMap<>();
            params.put("tableName", model.getMemoryTable());
            params.put("pageNum", "1");
            params.put("pageSize", "1000");

            String url = "http://" + model.getNodeIp().getIp() + ":" + model.getNodeIp().getPort() + "/localCache/getCacheByPage";

            JSONObject cacheData = null;
            try {
                cacheData = SeeRequest.getCacheData(url, params);
            } catch (IOException | URISyntaxException ex) {
                handleError(ex);
            }

            if (cacheData != null && "true".equals(cacheData.getString("success"))) {
                JSONObject jsonData = cacheData.getJSONObject("data").getJSONObject("data");

                Set<String> rowKeys = jsonData.keySet();
                String[][] table = new String[rowKeys.size()][];
                String[] columnNames = null;

                int rowIndex = 0;
                for (String rowKey : rowKeys) {
                    JSONArray rowArray = jsonData.getJSONArray(rowKey);
                    JSONObject firstRowObject = rowArray.getJSONObject(0);
                    Set<String> columnKeys = firstRowObject.keySet();

                    if (columnNames == null) {
                        columnNames = columnKeys.toArray(new String[0]);
                    }

                    String[] rowData = new String[columnKeys.size()];
                    int colIndex = 0;
                    for (String columnKey : columnKeys) {
                        rowData[colIndex++] = firstRowObject.getString(columnKey);
                    }
                    table[rowIndex++] = rowData;
                }

                table1.setModel(new javax.swing.table.DefaultTableModel(table, columnNames));
                table1.setDefaultEditor(Object.class, null);
            }

        });
    }

    public JPanel getPanel() {
        return panel;
    }

    // 刷新微服务列表
    private void refreshMacroSvr(SeeConfig seeConfig) {
        macroSvr.removeAllItems();
        nodeIp.removeAllItems();
        memoryTable.removeAllItems();
        dicts = new HashMap<>();
        auth = "";

        // 更新 UI 必须在事件调度线程上进行
        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    SeeRequest.login(seeConfig);
                    auth = SeeRequest.getAuth(seeConfig);
                    return SeeRequest.getUf30AndXoneApps(seeConfig, auth);
                } catch (Exception ex) {
                    handleError(ex);
                    return null;
                }
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps != null) {
                    macroSvr.removeAllItems();
                    JSONArray data = uf30AndXoneApps.getJSONArray("data");

                    Map<String, String> dict = new HashMap<>();
                    macroSvr.addItem(null);
                    for (int i = 0; i < data.size(); i++) {
                        JSONObject jsonObject = data.getJSONObject(i);
                        macroSvr.addItem(jsonObject.getString("name"));
                        dict.put(jsonObject.getString("name"), jsonObject.getString("id"));
                    }
                    dicts.put("macroSvr", dict);
                }
            }).exceptionally(ex -> {
                log.error("刷新微服务失败：{}", ex.getMessage(), ex);
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
                        return SeeRequest.getLocalCacheFormDataOnlyComputer(seeConfig, auth, value);
                    } catch (IOException | URISyntaxException e) {
                        handleError(e);
                        return null;
                    }
                }
                return null;
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps != null) {
                    JSONArray data = uf30AndXoneApps.getJSONArray("data");
                    Map<String, String> dict = new HashMap<>();
                    nodeIp.addItem(null);
                    for (int i = 0; i < data.size(); i++) {
                        JSONObject jsonObject = data.getJSONObject(i);
                        nodeIp.addItem(jsonObject.getString("address"));
                        dict.put(jsonObject.getString("address"), jsonObject.getString("address") + ":" + jsonObject.getString("httpPort"));
                    }
                    dicts.put("nodeIp", dict);
                }
            }).exceptionally(ex -> {
                log.error("刷新节点IP失败：{}", ex.getMessage(), ex);
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
                        return SeeRequest.getLocalCacheFormDataOnlyTable(seeConfig, auth, split[0], split[1]);
                    } catch (IOException | URISyntaxException e) {
                        handleError(e);
                        return null;
                    }
                }
                return null;
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps != null) {
                    JSONArray data = uf30AndXoneApps.getJSONArray("data");
                    Map<String, String> dict = new HashMap<>();
                    memoryTable.addItem(null);
                    for (int i = 0; i < data.size(); i++) {
                        String table = data.getString(i);
                        memoryTable.addItem(table);
                        dict.put(table, table);
                    }
                    dicts.put("memoryTable", dict);
                }
            }).exceptionally(ex -> {
                log.error("刷新内存表失败：{}", ex.getMessage(), ex);
                return null;
            });
        });
    }

    private void handleError(Exception ex) {
        log.error("操作失败：{}", ex.getMessage(), ex);
        JOptionPane.showMessageDialog(panel, "操作失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }

    class Model {
        private String env;
        private String macroName;
        private NodeIp nodeIp;
        private String memoryTable;
        private String condition;

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public String getMacroName() {
            return macroName;
        }

        public void setMacroName(String macroName) {
            this.macroName = macroName;
        }

        public NodeIp getNodeIp() {
            return nodeIp;
        }

        public void setNodeIp(NodeIp nodeIp) {
            this.nodeIp = nodeIp;
        }

        public String getMemoryTable() {
            return memoryTable;
        }

        public void setMemoryTable(String memoryTable) {
            this.memoryTable = memoryTable;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }
    }

    class NodeIp {
        private String ip;
        private String port;

        public NodeIp(String ip, String port) {
            this.ip = ip;
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
    }
}
