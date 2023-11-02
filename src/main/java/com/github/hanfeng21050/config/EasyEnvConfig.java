package com.github.hanfeng21050.config;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @Author hanfeng32305
 * @Date 2023/10/30 17:12
 */
public class EasyEnvConfig {
    private Map<String, CustomValue> customMap;

    public Map<String, CustomValue> getCustomMap() {
        if (customMap == null) {
            customMap = new TreeMap<>();
        }
        return customMap;
    }

    public void setCustomMap(Map<String, CustomValue> customMap) {
        this.customMap = customMap;
    }

    public static class CustomValue {

        private String label;

        private String address;
        /**
         * ¿‡–Õ
         */
        private String username;
        /**
         * ÷µ
         */
        private String password;

        public CustomValue() {
        }

        public CustomValue(String label, String address, String username, String password) {
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
}
