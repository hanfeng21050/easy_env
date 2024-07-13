package com.github.hanfeng21050.extensions.ToolWindow;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.hanfeng21050.config.EasyEnvConfig;
import com.github.hanfeng21050.config.SeeConfig;
import com.github.hanfeng21050.request.SeeRequest;
import com.intellij.openapi.application.ApplicationManager;
import org.apache.commons.lang.StringUtils;
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
    List<ServerInfo> serverInfos;
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
    private String appId = "";

    /**
     * 构造事件
     *
     * @param easyEnvConfig
     */
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
        addListeners();
    }

    public JPanel getPanel() {
        return panel;
    }

    /**
     * 初始化组件
     */
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
                });
            }
        }
    }

    /**
     * 设置事件
     */
    private void addListeners() {
        // 环境选择框事件
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

        // 微服务选择框事件
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

        // 节点IP选择框事件
        nodeIp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) env.getSelectedItem();
                String nodeIpSelectedItem = (String) nodeIp.getSelectedItem();
                model.setNodeIp(nodeIpSelectedItem);

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

        // 内存表选择框事件
        memoryTable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) memoryTable.getSelectedItem();
                model.setMemoryTable(selectedItem);
            }
        });
    }

    /**
     * 查询
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    private void performQuery() throws IOException, URISyntaxException {
        String envItem = (String) env.getSelectedItem();
        String macroSvrItem = (String) macroSvr.getSelectedItem();
        String nodeIpItem = (String) nodeIp.getSelectedItem();
        String memoryTableItem = (String) memoryTable.getSelectedItem();

        if (StringUtils.isBlank(envItem) || StringUtils.isBlank(macroSvrItem) || StringUtils.isBlank(nodeIpItem) || StringUtils.isBlank(memoryTableItem)) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Map<String, String> params = new HashMap<>();
            params.put("tableName", model.getMemoryTable());
            params.put("pageNum", "1");
            params.put("pageSize", "1000");

            String[] split = model.getNodeIp().split(":");

            String url = "http://" + split[0].trim() + ":" + split[1].trim() + "/localCache/getCacheByPage";

            JSONObject cacheData = null;
            try {
                cacheData = SeeRequest.getCacheData(url, params);
            } catch (IOException | URISyntaxException ex) {
                handleError(ex);
            }

            if (cacheData != null && "true".equals(cacheData.getString("success"))) {
                JSONObject jsonData = cacheData.getJSONObject("data").getJSONObject("data");
                setTableData(jsonData, condition.getText());
                this.model.setTableData(jsonData);
            }
        });
    }

    /**
     * 刷新微服务列表
     *
     * @param seeConfig
     */
    private void refreshMacroSvr(SeeConfig seeConfig) {
        macroSvr.removeAllItems();
        nodeIp.removeAllItems();
        memoryTable.removeAllItems();
        auth = "";

        // 更新 UI 必须在事件调度线程上进行
        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    SeeRequest.login(seeConfig);
                    auth = SeeRequest.getAuth(seeConfig);
                    appId = SeeRequest.getApplication(seeConfig, "服务控制台", auth);
                    JSONObject serviceList = SeeRequest.getServiceList(seeConfig, appId, auth);

                    serverInfos = new ArrayList<>();
                    if (serviceList != null && serviceList.getString("message").equals("success")) {
                        JSONArray jsonArray = serviceList.getJSONObject("data").getJSONArray("data_list");
                        for (int i = 0; i < jsonArray.size(); i++) {
                            ServerInfo serverInfo = new ServerInfo();
                            JSONObject server = jsonArray.getJSONObject(i);

                            serverInfo.setMacroName(server.getString("service_name"));
                            serverInfo.setGroup(server.getString("group"));
                            serverInfo.setVersion(server.getString("version"));
                            serverInfos.add(serverInfo);
                        }
                    }

                    return serverInfos;
                } catch (Exception ex) {
                    handleError(ex);
                    return null;
                }
            }).thenAccept(serviceList -> {
                if (!serviceList.isEmpty()) {
                    macroSvr.removeAllItems();
                    macroSvr.addItem(null);
                    for (ServerInfo serverInfo : serverInfos) {
                        macroSvr.addItem(serverInfo.getMacroName());
                    }
                }
            }).exceptionally(ex -> {
                log.error("刷新微服务失败：{}", ex.getMessage(), ex);
                return null;
            });
        });
    }

    /**
     * 刷新节点IP
     *
     * @param seeConfig
     */
    private void refreshNodeIp(SeeConfig seeConfig) {
        nodeIp.removeAllItems();
        memoryTable.removeAllItems();

        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String value = (String) macroSvr.getSelectedItem();
                    if (value != null) {
                        Optional<ServerInfo> any = serverInfos.stream().filter(e -> e.getMacroName().equals(value)).findAny();
                        if (any.isPresent()) {
                            ServerInfo serverInfo = any.get();
                            Map<String, String> body = new HashMap<>();
                            body.put("service_name", serverInfo.getMacroName());
                            body.put("group", serverInfo.getGroup());
                            body.put("version", serverInfo.getVersion());
                            body.put("m_pid", "pid");
                            body.put("appId", appId);
                            JSONObject serviceInfo = SeeRequest.getServiceInfo(seeConfig, auth, body);

                            JSONArray data = serviceInfo.getJSONObject("data").getJSONArray("data");
                            List<String> nodeIps = new ArrayList<>();
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                String protocol = item.getString("protocol");
                                if (protocol.equals("http")) {
                                    nodeIps.add(item.getString("addr"));
                                }
                            }
                            return nodeIps;
                        }
                    }
                } catch (Exception ex) {
                    handleError(ex);
                    return null;
                }
                return null;
            }).thenAccept(nodeIps -> {
                if (!nodeIps.isEmpty()) {
                    nodeIp.removeAllItems();
                    nodeIp.addItem(null);
                    for (String ip : nodeIps) {
                        nodeIp.addItem(ip);
                    }
                }
            }).exceptionally(ex -> {
                log.error("刷新节点IP失败：{}", ex.getMessage(), ex);
                return null;
            });
        });
    }

    /**
     * 刷新内存表
     *
     * @param seeConfig
     */
    private void refreshMemoryTable(SeeConfig seeConfig) {
        memoryTable.removeAllItems();
        // 更新 UI 必须在事件调度线程上进行
        ApplicationManager.getApplication().invokeLater(() -> {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String nodeIp = model.getNodeIp();
                    if (StringUtils.isNotBlank(nodeIp)) {
                        String[] split = nodeIp.split(":");
                        JSONObject localCacheFormDataOnlyTable = SeeRequest.getLocalCacheFormDataOnlyTable(seeConfig, auth, split[0].trim(), split[1].trim());
                        return localCacheFormDataOnlyTable.getJSONArray("data");
                    }
                } catch (IOException | URISyntaxException e) {
                    handleError(e);
                    return null;
                }
                return null;
            }).thenAccept(data -> {
                if (data != null) {
                    memoryTable.addItem(null);
                    for (int i = 0; i < data.size(); i++) {
                        String table = data.getString(i);
                        memoryTable.addItem(table);
                    }
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

    public void setTableData(JSONObject jsonData, String condition) {
        Set<String> rowKeys = jsonData.keySet();

        if (rowKeys.isEmpty()) {
            table1.setModel(new javax.swing.table.DefaultTableModel(new String[0][0], new String[0]));
            table1.setDefaultEditor(Object.class, null);
            return;
        }

        List<List<String>> arr = new ArrayList<>();
        List<String> header = new ArrayList<>();
        int rowIndex = 0;
        for (String rowKey : rowKeys) {
            if (rowKey.equals("all#")) {
                continue;
            }

            JSONArray rowArray = jsonData.getJSONArray(rowKey);
            JSONObject firstRowObject = rowArray.getJSONObject(0);
            Set<String> columnKeys = firstRowObject.keySet();
            String[] split = rowKey.split("#");


            List<String> row = new ArrayList<>();
            for (String columnKey : columnKeys) {
                if (rowIndex == 0) {
                    String index = "";
                    for (int i = 0; i < split.length; i++) {
                        if (i % 2 == 0 && columnKey.replaceAll("_", "").equalsIgnoreCase(split[i].replaceAll("_", ""))) {
                            index = "*";
                            break;
                        }
                    }
                    header.add(index + columnKey);
                }

                if (StringUtils.isNotBlank(condition)) {
                    boolean isFilter = true;
                    for (int i = 0; i < split.length; i++) {
                        if (i % 2 == 1 && split[i].contains(condition)) {
                            isFilter = false;
                            break;
                        }
                    }
                    if (isFilter) {
                        continue;
                    }
                }
                row.add(firstRowObject.getString(columnKey));
            }
            if (!row.isEmpty()) {
                arr.add(row);
            }
            rowIndex++;
        }
        table1.setModel(new javax.swing.table.DefaultTableModel(listTo2DArray(arr), header.toArray()));
        table1.setDefaultEditor(Object.class, null);
    }

    public static String[][] listTo2DArray(List<List<String>> list) {
        if (list == null || list.isEmpty()) {
            return new String[0][0];
        }

        int rows = list.size();
        int cols = list.get(0).size();
        String[][] array = new String[rows][cols];

        for (int i = 0; i < rows; i++) {
            List<String> rowList = list.get(i);
            for (int j = 0; j < cols; j++) {
                array[i][j] = rowList.get(j);
            }
        }

        return array;
    }

    static class Model {
        private String env;
        private String macroName;
        private String nodeIp;
        private String memoryTable;
        private String condition;

        public JSONObject getTableData() {
            return tableData;
        }

        public void setTableData(JSONObject tableData) {
            this.tableData = tableData;
        }

        private JSONObject tableData;

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

        public String getNodeIp() {
            return nodeIp;
        }

        public void setNodeIp(String nodeIp) {
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


    static class ServerInfo {
        private String macroName;
        private String group;
        private String version;

        public String getMacroName() {
            return macroName;
        }

        public void setMacroName(String macroName) {
            this.macroName = macroName;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
