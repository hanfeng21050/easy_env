package com.github.hanfeng21050.dialog;

import com.github.hanfeng21050.model.MenuFunctionData;
import com.github.hanfeng21050.utils.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 菜单树对话框
 * 用于展示和选择菜单及功能号
 */
public class MenuTreeDialog extends DialogWrapper {
    private final Project project;
    private final IndependentCheckboxTree tree;
    private final IndependentCheckboxTree.IndependentTreeNode root;
    private final MenuFunctionData menuData;
    private final Map<String, MenuFunctionData.InfoItem> functionMap;

    public MenuTreeDialog(Project project, MenuFunctionData menuData) {
        super(project);
        setTitle("Oracle菜单功能脚本");
        this.project = project;
        this.menuData = menuData;
        this.functionMap = new HashMap<>();

        // 构建功能号映射表
        if (menuData.getDetail() != null && menuData.getDetail().getInfos() != null) {
            for (MenuFunctionData.InfoItem info : menuData.getDetail().getInfos()) {
                functionMap.put(info.getUuid(), info);
            }
        }

        // 创建树根节点
        root = new IndependentCheckboxTree.IndependentTreeNode("菜单功能");
        root.setChecked(false);

        // 创建独立的复选框树
        tree = new IndependentCheckboxTree(root);

        // 构建菜单树
        buildMenuTree();

        // 设置对话框
        init();
        setSize(700, 600);
    }

    @Override
    protected void doOKAction() {
        List<MenuTreeNodeData> selectedItems = getSelectedItems();
        if (selectedItems.isEmpty()) {
            Messages.showWarningDialog(
                    "请至少选择一个菜单或功能号",
                    "警告"
            );
            return;
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 添加树控件
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(680, 500));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 全部展开树的前两层
//        expandTopLevels();
        
        return panel;
    }

    /**
     * 构建菜单树
     */
    private void buildMenuTree() {
        if (menuData.getDetail() == null || menuData.getDetail().getItems() == null) {
            Logger.info("菜单数据为空");
            return;
        }

        List<MenuFunctionData.MenuItem> menuItems = menuData.getDetail().getItems();
        Logger.info("开始构建菜单树，菜单项总数: " + menuItems.size());

        // 清空根节点
        root.removeAllChildren();

        // 第一步：递归处理所有菜单项（包括children中的）并创建节点映射
        Map<String, IndependentCheckboxTree.IndependentTreeNode> allMenuNodeMap = new HashMap<>();
        Set<String> allProcessedNodes = new HashSet<>();

        Logger.info("=== 开始递归创建所有菜单节点 ===");
        for (MenuFunctionData.MenuItem menuItem : menuItems) {
            createAllMenuNodesRecursively(menuItem, allMenuNodeMap, 0);
        }

        // 第二步：为所有菜单节点添加功能号
        Logger.info("=== 开始为所有菜单添加功能号 ===");
        addFunctionNodesToAllMenus(allMenuNodeMap);

        // 第三步：构建树结构 - 递归处理children关系
        Logger.info("=== 开始构建菜单树结构 ===");
        for (MenuFunctionData.MenuItem menuItem : menuItems) {
            buildMenuStructureRecursively(menuItem, allMenuNodeMap, allProcessedNodes, 0);
        }

        // 第四步：添加根级菜单（顶层的menuItems）
        for (MenuFunctionData.MenuItem menuItem : menuItems) {
            IndependentCheckboxTree.IndependentTreeNode menuNode = allMenuNodeMap.get(menuItem.getUuid());
            if (menuNode != null && !allProcessedNodes.contains(menuItem.getUuid())) {
                root.add(menuNode);
                allProcessedNodes.add(menuItem.getUuid());
                Logger.info("添加根级菜单: " + getMenuName(menuItem));
            }
        }

        // 第五步：对所有节点进行排序
        sortAllTreeNodes(root);

        // 刷新树模型
        ((DefaultTreeModel) tree.getModel()).reload();

        Logger.info("菜单树构建完成，根节点数量: " + root.getChildCount());
        Logger.info("总菜单节点数量: " + allMenuNodeMap.size());
        logTreeStructure(root, 0);
    }

    /**
     * 递归创建所有菜单节点（包括children中的）
     */
    private void createAllMenuNodesRecursively(MenuFunctionData.MenuItem menuItem,
                                               Map<String, IndependentCheckboxTree.IndependentTreeNode> menuNodeMap,
                                               int level) {
        String indent = "  ".repeat(level);

        // 为当前菜单项创建节点
        if (!menuNodeMap.containsKey(menuItem.getUuid())) {
            IndependentCheckboxTree.IndependentTreeNode menuNode =
                    new IndependentCheckboxTree.IndependentTreeNode(new MenuTreeNodeData(menuItem, true));
            menuNode.setChecked(false);
            menuNodeMap.put(menuItem.getUuid(), menuNode);
            Logger.info(indent + "创建菜单节点: " + getMenuName(menuItem) + " UUID: " + menuItem.getUuid() +
                    " children数量: " + (menuItem.getChildren() != null ? menuItem.getChildren().size() : 0) +
                    " slaves数量: " + (menuItem.getSlaves() != null ? menuItem.getSlaves().size() : 0));
        }

        // 递归处理children中的菜单项
        if (menuItem.getChildren() != null && !menuItem.getChildren().isEmpty()) {
            for (MenuFunctionData.MenuItem child : menuItem.getChildren()) {
                createAllMenuNodesRecursively(child, menuNodeMap, level + 1);
            }
        }
    }

    /**
     * 为所有菜单节点添加功能号
     */
    private void addFunctionNodesToAllMenus(Map<String, IndependentCheckboxTree.IndependentTreeNode> menuNodeMap) {
        for (Map.Entry<String, IndependentCheckboxTree.IndependentTreeNode> entry : menuNodeMap.entrySet()) {
            IndependentCheckboxTree.IndependentTreeNode menuNode = entry.getValue();
            MenuTreeNodeData nodeData = (MenuTreeNodeData) menuNode.getUserObject();
            MenuFunctionData.MenuItem menuItem = nodeData.getMenuItem();

            if (menuItem.getSlaves() != null && !menuItem.getSlaves().isEmpty()) {
                Logger.info("为菜单 " + getMenuName(menuItem) + " 添加 " + menuItem.getSlaves().size() + " 个功能号");
                for (MenuFunctionData.MenuItem.Slave slave : menuItem.getSlaves()) {
                    String functionId = getFunctionId(slave);
                    if (functionId != null) {
                        MenuFunctionData.InfoItem functionInfo = functionMap.get(functionId);
                        if (functionInfo != null) {
                            IndependentCheckboxTree.IndependentTreeNode functionNode =
                                    new IndependentCheckboxTree.IndependentTreeNode(new MenuTreeNodeData(functionInfo, false));
                            functionNode.setChecked(false);
                            menuNode.add(functionNode);
                        }
                    }
                }
            }
        }
    }

    /**
     * 递归构建菜单结构（处理children关系）
     */
    private void buildMenuStructureRecursively(MenuFunctionData.MenuItem menuItem,
                                               Map<String, IndependentCheckboxTree.IndependentTreeNode> menuNodeMap,
                                               Set<String> processedNodes,
                                               int level) {
        String indent = "  ".repeat(level);
        IndependentCheckboxTree.IndependentTreeNode parentNode = menuNodeMap.get(menuItem.getUuid());
        
        if (menuItem.getChildren() != null && !menuItem.getChildren().isEmpty()) {
            Logger.info(indent + "处理菜单 " + getMenuName(menuItem) + " 的 " + menuItem.getChildren().size() + " 个子菜单");

            for (MenuFunctionData.MenuItem child : menuItem.getChildren()) {
                IndependentCheckboxTree.IndependentTreeNode childNode = menuNodeMap.get(child.getUuid());
                if (childNode != null && parentNode != null) {
                    parentNode.add(childNode);
                    processedNodes.add(child.getUuid());
                    Logger.info(indent + "  添加子菜单: " + getMenuName(child) + " 到父菜单 " + getMenuName(menuItem) + " 下");

                    // 递归处理子菜单的children
                    buildMenuStructureRecursively(child, menuNodeMap, processedNodes, level + 1);
                }
            }
        }
    }

    /**
     * 获取菜单名称用于日志
     */
    private String getMenuName(MenuFunctionData.MenuItem menuItem) {
        if (menuItem != null && menuItem.getExtensibleModel() != null &&
                menuItem.getExtensibleModel().getData() != null) {
            return menuItem.getExtensibleModel().getData().getMenuName();
        }
        return "未知菜单";
    }

    /**
     * 获取功能号ID
     */
    private String getFunctionId(MenuFunctionData.MenuItem.Slave slave) {
        if (slave.getExtensibleModel() != null &&
                slave.getExtensibleModel().getData() != null) {
            return slave.getExtensibleModel().getData().getFunctionId();
        }
        return null;
    }

    /**
     * 获取父菜单ID
     */
    private String getParentId(MenuFunctionData.MenuItem menuItem) {
        if (menuItem.getExtensibleModel() != null &&
                menuItem.getExtensibleModel().getData() != null) {
            return menuItem.getExtensibleModel().getData().getParentId();
        }
        return null;
    }

    /**
     * 递归排序所有树节点
     */
    private void sortAllTreeNodes(IndependentCheckboxTree.IndependentTreeNode node) {
        if (node.getChildCount() <= 1) {
            return;
        }

        // 收集子节点
        List<IndependentCheckboxTree.IndependentTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((IndependentCheckboxTree.IndependentTreeNode) node.getChildAt(i));
        }

        // 排序逻辑：菜单节点按order_no排序，功能号节点按名称排序
        children.sort((n1, n2) -> {
            Object obj1 = n1.getUserObject();
            Object obj2 = n2.getUserObject();

            if (obj1 instanceof MenuTreeNodeData && obj2 instanceof MenuTreeNodeData) {
                MenuTreeNodeData data1 = (MenuTreeNodeData) obj1;
                MenuTreeNodeData data2 = (MenuTreeNodeData) obj2;

                // 菜单节点排在功能号节点前面
                if (data1.isMenu() && !data2.isMenu()) return -1;
                if (!data1.isMenu() && data2.isMenu()) return 1;

                // 都是菜单节点，按order_no排序
                if (data1.isMenu() && data2.isMenu()) {
                    try {
                        int order1 = getOrderNo(data1.getMenuItem());
                        int order2 = getOrderNo(data2.getMenuItem());
                        if (order1 != order2) {
                            return Integer.compare(order1, order2);
                        }
                        // order_no相同时按名称排序
                        return data1.toString().compareTo(data2.toString());
                    } catch (Exception e) {
                        // 排序失败时按名称排序
                        return data1.toString().compareTo(data2.toString());
                    }
                }

                // 都是功能号节点，按名称排序
                if (!data1.isMenu() && !data2.isMenu()) {
                    return data1.toString().compareTo(data2.toString());
                }

                // 其他情况按字符串排序
                return data1.toString().compareTo(data2.toString());
            }

            return 0;
        });

        // 重新添加排序后的子节点
        node.removeAllChildren();
        for (IndependentCheckboxTree.IndependentTreeNode child : children) {
            node.add(child);
            // 递归排序子节点
            sortAllTreeNodes(child);
        }
    }

    /**
     * 获取菜单排序号
     */
    private int getOrderNo(MenuFunctionData.MenuItem menuItem) {
        if (menuItem != null && menuItem.getExtensibleModel() != null &&
                menuItem.getExtensibleModel().getData() != null) {
            String orderNo = menuItem.getExtensibleModel().getData().getOrderNo();
            if (orderNo != null && !orderNo.isEmpty()) {
                try {
                    return Integer.parseInt(orderNo);
                } catch (NumberFormatException e) {
                    // 如果解析失败，返回默认值
                }
            }
        }
        return 999; // 默认排序值
    }

    /**
     * 展开前两层节点
     */
    private void expandTopLevels() {
        tree.expandTopLevels();
    }

    /**
     * 记录树结构用于调试
     */
    private void logTreeStructure(IndependentCheckboxTree.IndependentTreeNode node, int level) {
        if (level > 4) return; // 只记录前4层

        String indent = "  ".repeat(level);
        Object userObject = node.getUserObject();

        if (userObject instanceof MenuTreeNodeData) {
            MenuTreeNodeData data = (MenuTreeNodeData) userObject;
            String type = data.isMenu() ? "📁菜单" : "⚙️功能号";
            Logger.info(indent + "├─ " + type + ": " + data.toString() + " (子节点: " + node.getChildCount() + ")");
        } else {
            Logger.info(indent + "🌳 根: " + userObject + " (子节点: " + node.getChildCount() + ")");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            IndependentCheckboxTree.IndependentTreeNode child = (IndependentCheckboxTree.IndependentTreeNode) node.getChildAt(i);
            logTreeStructure(child, level + 1);
        }
    }

    /**
     * 获取选中的菜单项
     */
    public List<MenuTreeNodeData> getSelectedItems() {
        List<MenuTreeNodeData> selectedItems = new ArrayList<>();
        List<IndependentCheckboxTree.IndependentTreeNode> checkedNodes = tree.getCheckedNodes();

        for (IndependentCheckboxTree.IndependentTreeNode node : checkedNodes) {
            Object userObject = node.getUserObject();
            if (userObject instanceof MenuTreeNodeData) {
                selectedItems.add((MenuTreeNodeData) userObject);
            }
        }
        
        return selectedItems;
    }

    /**
     * 菜单树节点数据
     */
    public static class MenuTreeNodeData {
        private final boolean isMenu;
        private final MenuFunctionData.MenuItem menuItem;
        private final MenuFunctionData.InfoItem functionItem;

        public MenuTreeNodeData(MenuFunctionData.MenuItem menuItem, boolean isMenu) {
            this.isMenu = isMenu;
            this.menuItem = menuItem;
            this.functionItem = null;
        }

        public MenuTreeNodeData(MenuFunctionData.InfoItem functionItem, boolean isMenu) {
            this.isMenu = isMenu;
            this.menuItem = null;
            this.functionItem = functionItem;
        }

        public boolean isMenu() {
            return isMenu;
        }

        public MenuFunctionData.MenuItem getMenuItem() {
            return menuItem;
        }

        public MenuFunctionData.InfoItem getFunctionItem() {
            return functionItem;
        }

        @Override
        public String toString() {
            if (isMenu && menuItem != null && menuItem.getExtensibleModel() != null && 
                menuItem.getExtensibleModel().getData() != null) {
                String menuName = menuItem.getExtensibleModel().getData().getMenuName();
                String menuCode = menuItem.getExtensibleModel().getData().getMenuCode();
                if (menuName != null && !menuName.isEmpty()) {
                    return menuCode != null && !menuCode.isEmpty() ?
                            menuName + " [" + menuCode + "]" : menuName;
                }
                return menuCode != null ? "[" + menuCode + "]" : "未命名菜单";
            } else if (!isMenu && functionItem != null && functionItem.getExtensibleModel() != null && 
                      functionItem.getExtensibleModel().getData() != null) {
                String name = functionItem.getExtensibleModel().getData().getSubTransName();
                String code = functionItem.getExtensibleModel().getData().getSubTransCode();
                if (name != null && !name.isEmpty()) {
                    return code != null && !code.isEmpty() ?
                            name + " [" + code + "]" : name;
                }
                return code != null ? "[" + code + "]" : "未命名功能";
            }
            return "未知项目";
        }
    }

}

