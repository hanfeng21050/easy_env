package com.github.hanfeng21050.model;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * 菜单功能数据模型
 */
public class MenuFunctionData {
    private Detail detail;

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }

    public static class Detail {
        private List<InfoItem> infos;
        private List<MenuItem> items;

        public List<InfoItem> getInfos() {
            return infos;
        }

        public void setInfos(List<InfoItem> infos) {
            this.infos = infos;
        }

        public List<MenuItem> getItems() {
            return items;
        }

        public void setItems(List<MenuItem> items) {
            this.items = items;
        }
    }

    /**
     * 功能号信息
     */
    public static class InfoItem {
        private String cate;
        private String cateUuid;
        private Customer customer;
        private boolean enable;
        private ExtensibleModel extensibleModel;
        private int status;
        private String uuid;

        // Getters and Setters
        public String getCate() {
            return cate;
        }

        public void setCate(String cate) {
            this.cate = cate;
        }

        public String getCateUuid() {
            return cateUuid;
        }

        public void setCateUuid(String cateUuid) {
            this.cateUuid = cateUuid;
        }

        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public ExtensibleModel getExtensibleModel() {
            return extensibleModel;
        }

        public void setExtensibleModel(ExtensibleModel extensibleModel) {
            this.extensibleModel = extensibleModel;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public static class ExtensibleModel {
            private InfoData data;
            private int status;

            public InfoData getData() {
                return data;
            }

            public void setData(InfoData data) {
                this.data = data;
            }

            public int getStatus() {
                return status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public static class InfoData {
                @JSONField(name = "sub_trans_name")
                private String subTransName;
                @JSONField(name = "sub_trans_code")
                private String subTransCode;
                @JSONField(name = "right_type")
                private String rightType;
                @JSONField(name = "password_type")
                private String passwordType;
                @JSONField(name = "ctrl_flag")
                private String ctrlFlag;
                @JSONField(name = "login_flag")
                private String loginFlag;
                @JSONField(name = "micro_service")
                private String microService;

                // Getters and Setters
                public String getSubTransName() {
                    return subTransName;
                }

                public void setSubTransName(String subTransName) {
                    this.subTransName = subTransName;
                }

                public String getSubTransCode() {
                    return subTransCode;
                }

                public void setSubTransCode(String subTransCode) {
                    this.subTransCode = subTransCode;
                }

                public String getRightType() {
                    return rightType;
                }

                public void setRightType(String rightType) {
                    this.rightType = rightType;
                }

                public String getPasswordType() {
                    return passwordType;
                }

                public void setPasswordType(String passwordType) {
                    this.passwordType = passwordType;
                }

                public String getCtrlFlag() {
                    return ctrlFlag;
                }

                public void setCtrlFlag(String ctrlFlag) {
                    this.ctrlFlag = ctrlFlag;
                }

                public String getLoginFlag() {
                    return loginFlag;
                }

                public void setLoginFlag(String loginFlag) {
                    this.loginFlag = loginFlag;
                }

                public String getMicroService() {
                    return microService;
                }

                public void setMicroService(String microService) {
                    this.microService = microService;
                }
            }
        }
    }

    /**
     * 菜单项
     */
    public static class MenuItem {
        private Customer customer;
        private boolean enable;
        private ExtensibleModel extensibleModel;
        private List<Slave> slaves;
        private List<MenuItem> children;
        private int status;
        private String uuid;

        // Getters and Setters
        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public ExtensibleModel getExtensibleModel() {
            return extensibleModel;
        }

        public void setExtensibleModel(ExtensibleModel extensibleModel) {
            this.extensibleModel = extensibleModel;
        }

        public List<Slave> getSlaves() {
            return slaves;
        }

        public void setSlaves(List<Slave> slaves) {
            this.slaves = slaves;
        }

        public List<MenuItem> getChildren() {
            return children;
        }

        public void setChildren(List<MenuItem> children) {
            this.children = children;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public static class ExtensibleModel {
            private MenuData data;
            private int status;

            public MenuData getData() {
                return data;
            }

            public void setData(MenuData data) {
                this.data = data;
            }

            public int getStatus() {
                return status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public static class MenuData {
                @JSONField(name = "parent_id")
                private String parentId;
                @JSONField(name = "menu_name")
                private String menuName;
                @JSONField(name = "menu_code")
                private String menuCode;
                @JSONField(name = "kind_code")
                private String kindCode;
                @JSONField(name = "order_no")
                private String orderNo;
                @JSONField(name = "micro_service")
                private String microService;
                @JSONField(name = "is_hidden")
                private String isHidden;
                @JSONField(name = "parent_menu_name")
                private String parentMenuName;
                @JSONField(name = "licence_type")
                private String licenceType;

                // Getters and Setters
                public String getParentId() {
                    return parentId;
                }

                public void setParentId(String parentId) {
                    this.parentId = parentId;
                }

                public String getMenuName() {
                    return menuName;
                }

                public void setMenuName(String menuName) {
                    this.menuName = menuName;
                }

                public String getMenuCode() {
                    return menuCode;
                }

                public void setMenuCode(String menuCode) {
                    this.menuCode = menuCode;
                }

                public String getKindCode() {
                    return kindCode;
                }

                public void setKindCode(String kindCode) {
                    this.kindCode = kindCode;
                }

                public String getOrderNo() {
                    return orderNo;
                }

                public void setOrderNo(String orderNo) {
                    this.orderNo = orderNo;
                }

                public String getMicroService() {
                    return microService;
                }

                public void setMicroService(String microService) {
                    this.microService = microService;
                }

                public String getIsHidden() {
                    return isHidden;
                }

                public void setIsHidden(String isHidden) {
                    this.isHidden = isHidden;
                }

                public String getParentMenuName() {
                    return parentMenuName;
                }

                public void setParentMenuName(String parentMenuName) {
                    this.parentMenuName = parentMenuName;
                }

                public String getLicenceType() {
                    return licenceType;
                }

                public void setLicenceType(String licenceType) {
                    this.licenceType = licenceType;
                }
            }
        }

        /**
         * 从属功能号
         */
        public static class Slave {
            private Customer customer;
            private boolean enable;
            private ExtensibleModel extensibleModel;
            private int status;
            private String uuid;

            public Customer getCustomer() {
                return customer;
            }

            public void setCustomer(Customer customer) {
                this.customer = customer;
            }

            public boolean isEnable() {
                return enable;
            }

            public void setEnable(boolean enable) {
                this.enable = enable;
            }

            public ExtensibleModel getExtensibleModel() {
                return extensibleModel;
            }

            public void setExtensibleModel(ExtensibleModel extensibleModel) {
                this.extensibleModel = extensibleModel;
            }

            public int getStatus() {
                return status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public String getUuid() {
                return uuid;
            }

            public void setUuid(String uuid) {
                this.uuid = uuid;
            }

            public static class ExtensibleModel {
                private SlaveData data;
                private int status;

                public SlaveData getData() {
                    return data;
                }

                public void setData(SlaveData data) {
                    this.data = data;
                }

                public int getStatus() {
                    return status;
                }

                public void setStatus(int status) {
                    this.status = status;
                }

                public static class SlaveData {
                    @JSONField(name = "origin_sub_trans_code")
                    private String originSubTransCode;
                    @JSONField(name = "function_id")
                    private String functionId;

                    public String getOriginSubTransCode() {
                        return originSubTransCode;
                    }

                    public void setOriginSubTransCode(String originSubTransCode) {
                        this.originSubTransCode = originSubTransCode;
                    }

                    public String getFunctionId() {
                        return functionId;
                    }

                    public void setFunctionId(String functionId) {
                        this.functionId = functionId;
                    }
                }
            }
        }
    }

    /**
     * 客户信息
     */
    public static class Customer {
        @JSONField(name = "public")
        private boolean isPublic;
        private int status;
        private String uuid;

        public boolean isPublic() {
            return isPublic;
        }

        public void setPublic(boolean isPublic) {
            this.isPublic = isPublic;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }
}

