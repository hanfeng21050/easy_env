package com.github.hanfeng21050.config;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:12
 */
public class EasyEnvConfig {
    private Map<String, SeeConnectInfo> seeConnectInfoMap;
    private Map<String, ConfigReplaceRule> configReplaceRuleMap;
    private SortedMap<String, String> excludedFileMap;

    public Map<String, SeeConnectInfo> getSeeConnectInfoMap() {
        if (seeConnectInfoMap == null) {
            seeConnectInfoMap = new TreeMap<>();
        }
        return seeConnectInfoMap;
    }

    public Map<String, ConfigReplaceRule> getConfigReplaceRuleMap() {
        if (configReplaceRuleMap == null) {
            configReplaceRuleMap = new TreeMap<>();
        }
        return configReplaceRuleMap;
    }

    public SortedMap<String, String> getExcludedFileMap() {
        if (excludedFileMap == null) {
            excludedFileMap = new TreeMap<>();
        }
        return excludedFileMap;
    }

    public void setSeeConnectInfoMap(Map<String, SeeConnectInfo> seeConnectInfoMap) {
        this.seeConnectInfoMap = seeConnectInfoMap;
    }

    public void setConfigReplaceRuleMap(Map<String, ConfigReplaceRule> configReplaceRuleMap) {
        this.configReplaceRuleMap = configReplaceRuleMap;
    }

    public void setExcludedFileMap(SortedMap<String, String> excludedFileMap) {
        this.excludedFileMap = excludedFileMap;
    }

    /**
     * see连接信息
     */
    public static class SeeConnectInfo {

        private String label = "";

        private String address = "";
        /**
         * 类型
         */
        private String username = "";
        /**
         * 值
         */
        private String password = "";

        public SeeConnectInfo() {
        }

        public SeeConnectInfo(String label, String address, String username, String password) {
            this.username = username;
            this.password = password;
            this.label = label;
            this.address = address;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    /**
     * 正则替换规则
     */
    public static class ConfigReplaceRule {
        private String fileName = "";
        private String regExpression = "";
        private String replaceStr = "";

        public ConfigReplaceRule() {
        }

        public ConfigReplaceRule(String fileName, String regExpression, String replaceStr) {
            this.fileName = fileName;
            this.regExpression = regExpression;
            this.replaceStr = replaceStr;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getRegExpression() {
            return regExpression;
        }

        public void setRegExpression(String regExpression) {
            this.regExpression = regExpression;
        }

        public String getReplaceStr() {
            return replaceStr;
        }

        public void setReplaceStr(String replaceStr) {
            this.replaceStr = replaceStr;
        }
    }
}
