package com.github.hanfeng21050.config;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:12
 */
@XmlRootElement
public class EasyEnvConfig implements Serializable {
    private List<SeeConnectInfo> seeConnectInfos;
    private List<ConfigReplaceRule> configReplaceRules;
    private List<ExcludedFile> excludedFiles;

    public List<SeeConnectInfo> getSeeConnectInfos() {
        if (seeConnectInfos == null) {
            seeConnectInfos = new ArrayList<>();
        }
        return seeConnectInfos;
    }

    public void setSeeConnectInfos(List<SeeConnectInfo> seeConnectInfos) {
        this.seeConnectInfos = seeConnectInfos;
    }

    public List<ConfigReplaceRule> getConfigReplaceRules() {
        if (configReplaceRules == null) {
            configReplaceRules = new ArrayList<>();
        }
        return configReplaceRules;
    }

    public void setConfigReplaceRules(List<ConfigReplaceRule> configReplaceRules) {
        this.configReplaceRules = configReplaceRules;
    }

    public List<ExcludedFile> getExcludedFiles() {
        if (excludedFiles == null) {
            excludedFiles = new ArrayList<>();
        }
        return excludedFiles;
    }

    public void setExcludedFiles(List<ExcludedFile> excludedFiles) {
        this.excludedFiles = excludedFiles;
    }

    /**
     * see连接信息
     */
    public static class SeeConnectInfo implements Serializable {
        private String uuid;

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

        public SeeConnectInfo(String uuid, String label, String address, String username, String password) {
            this.uuid = uuid;
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

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    /**
     * 正则替换规则
     */
    public static class ConfigReplaceRule implements Serializable {
        private String uuid;
        private String fileName = "";
        private String regExpression = "";
        private String replaceStr = "";

        public ConfigReplaceRule() {
        }

        public ConfigReplaceRule(String uuid, String fileName, String regExpression, String replaceStr) {
            this.uuid = uuid;
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

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    /**
     * 文件过滤
     */
    public static class ExcludedFile implements Serializable {
        private String uuid;
        private String fileName;

        public ExcludedFile() {
        }

        public ExcludedFile(String uuid, String fileName) {
            this.uuid = uuid;
            this.fileName = fileName;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
