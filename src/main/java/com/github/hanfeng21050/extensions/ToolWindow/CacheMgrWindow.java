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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    refreshMacroSvr(seeConfig);
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

        if(StringUtils.isBlank(envItem) || StringUtils.isBlank(macroSvrItem) || StringUtils.isBlank(nodeIpItem) || StringUtils.isBlank(memoryTableItem)) {
            return;
        }

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

                    if (data != null) {
                        Map<String, String> dict = new HashMap<>();
                        macroSvr.addItem(null);
                        for (int i = 0; i < data.size(); i++) {
                            JSONObject jsonObject = data.getJSONObject(i);
                            macroSvr.addItem(jsonObject.getString("name"));
                            dict.put(jsonObject.getString("name"), jsonObject.getString("id"));
                        }
                        dicts.put("macroSvr", dict);
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
                Map<String, String> nodeIpDict = dicts.get("nodeIp");
                if (nodeIpDict != null) {
                    String value = nodeIpDict.get((String) nodeIp.getSelectedItem());
                    if (value != null) {
                        String[] split = value.split(":");
                        try {
                            return SeeRequest.getLocalCacheFormDataOnlyTable(seeConfig, auth, split[0], split[1]);
                        } catch (IOException | URISyntaxException e) {
                            handleError(e);
                            return null;
                        }
                    }
                }
                return null;
            }).thenAccept(uf30AndXoneApps -> {
                if (uf30AndXoneApps != null) {
                    JSONArray data = uf30AndXoneApps.getJSONArray("data");
                    if (data != null) {
                        Map<String, String> dict = new HashMap<>();
                        memoryTable.addItem(null);
                        for (int i = 0; i < data.size(); i++) {
                            String table = data.getString(i);
                            memoryTable.addItem(table);
                            dict.put(table, table);
                        }
                        dicts.put("memoryTable", dict);
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
        List<List<String>> arr = new ArrayList<>();
        List<String> header = new ArrayList<>();
        int rowIndex = 0;
        for (String rowKey : rowKeys) {
            JSONArray rowArray = jsonData.getJSONArray(rowKey);
            JSONObject firstRowObject = rowArray.getJSONObject(0);
            Set<String> columnKeys = firstRowObject.keySet();
            String[] split = rowKey.split("#");


            List<String> row = new ArrayList<>();
            for (String columnKey : columnKeys) {
                if(rowIndex == 0) {
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
            rowIndex ++;
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

    public static String convertSnakeToCamel(String snakeCaseString) {
        if (snakeCaseString == null || snakeCaseString.isEmpty()) {
            return snakeCaseString;
        }

        StringBuilder camelCaseString = new StringBuilder();
        boolean nextCharUpperCase = false;

        for (char c : snakeCaseString.toCharArray()) {
            if (c == '_') {
                nextCharUpperCase = true;
            } else {
                if (nextCharUpperCase) {
                    camelCaseString.append(Character.toUpperCase(c));
                    nextCharUpperCase = false;
                } else {
                    camelCaseString.append(c);
                }
            }
        }

        return camelCaseString.toString();
    }

    static class Model {
        private String env;
        private String macroName;
        private NodeIp nodeIp;
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

    static class NodeIp {
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
