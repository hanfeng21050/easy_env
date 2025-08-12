package com.github.hanfeng21050.dialog;

import com.github.hanfeng21050.model.MenuFunctionData;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 菜单树对话框
 * 用于展示和选择菜单及功能号
 */
public class MenuTreeDialog extends DialogWrapper {
    private final Project project;
    private final CheckboxTree tree;
    private final CheckedTreeNode root;
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
        root = new CheckedTreeNode("菜单功能");
        root.setChecked(false);

        // 创建树控件
        tree = new CheckboxTree(new MenuTreeCellRenderer(), root);

        // 构建菜单树
        buildMenuTree();

        // 设置对话框
        init();
        setSize(600, 500);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // 添加说明标签
        JLabel label = new JLabel("选择需要的菜单和功能号（可单独选择，无联动）：");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(label, BorderLayout.NORTH);
        
        // 添加树控件
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(580, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 全部展开树
        expandAll();
        
        return panel;
    }

    /**
     * 构建菜单树
     */
    private void buildMenuTree() {
        if (menuData.getDetail() == null || menuData.getDetail().getItems() == null) {
            return;
        }

        // 创建菜单项的映射表
        Map<String, CheckedTreeNode> menuNodeMap = new HashMap<>();
        Map<String, List<MenuFunctionData.MenuItem>> childrenMap = new HashMap<>();

        // 第一遍：创建所有菜单节点并建立父子关系映射
        for (MenuFunctionData.MenuItem menuItem : menuData.getDetail().getItems()) {
            CheckedTreeNode menuNode = new CheckedTreeNode(new MenuTreeNodeData(menuItem, true));
            menuNode.setChecked(false);
            menuNodeMap.put(menuItem.getUuid(), menuNode);

            String parentId = menuItem.getExtensibleModel() != null && 
                            menuItem.getExtensibleModel().getData() != null ? 
                            menuItem.getExtensibleModel().getData().getParentId() : null;

            if (parentId != null && !parentId.isEmpty()) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menuItem);
            }
        }

        // 第二遍：构建树结构
        for (MenuFunctionData.MenuItem menuItem : menuData.getDetail().getItems()) {
            CheckedTreeNode menuNode = menuNodeMap.get(menuItem.getUuid());
            String parentId = menuItem.getExtensibleModel() != null && 
                            menuItem.getExtensibleModel().getData() != null ? 
                            menuItem.getExtensibleModel().getData().getParentId() : null;

            // 优先使用children属性，如果没有则使用parent_id构建的关系
            List<MenuFunctionData.MenuItem> children = menuItem.getChildren();
            if (children == null) {
                children = childrenMap.get(menuItem.getUuid());
            }

            // 添加子菜单
            if (children != null) {
                children.sort(Comparator.comparing(item -> {
                    try {
                        return Integer.parseInt(item.getExtensibleModel().getData().getOrderNo());
                    } catch (Exception e) {
                        return 999;
                    }
                }));
                
                for (MenuFunctionData.MenuItem child : children) {
                    CheckedTreeNode childNode = menuNodeMap.get(child.getUuid());
                    if (childNode != null) {
                        menuNode.add(childNode);
                    }
                }
            }

            // 添加功能号子节点
            if (menuItem.getSlaves() != null) {
                for (MenuFunctionData.MenuItem.Slave slave : menuItem.getSlaves()) {
                    String functionId = slave.getExtensibleModel() != null && 
                                      slave.getExtensibleModel().getData() != null ? 
                                      slave.getExtensibleModel().getData().getFunctionId() : null;
                    
                    if (functionId != null) {
                        MenuFunctionData.InfoItem functionInfo = functionMap.get(functionId);
                        if (functionInfo != null) {
                            CheckedTreeNode functionNode = new CheckedTreeNode(new MenuTreeNodeData(functionInfo, false));
                            functionNode.setChecked(false);
                            menuNode.add(functionNode);
                        }
                    }
                }
            }

            // 如果是根节点（没有父节点），添加到树根
            if (parentId == null || parentId.isEmpty() || !menuNodeMap.containsKey(parentId)) {
                root.add(menuNode);
            }
        }

        // 刷新树模型
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    /**
     * 展开所有节点
     */
    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * 获取选中的菜单项
     */
    public List<MenuTreeNodeData> getSelectedItems() {
        List<MenuTreeNodeData> selectedItems = new ArrayList<>();
        collectSelectedNodes(root, selectedItems);
        return selectedItems;
    }

    /**
     * 递归收集选中的节点
     */
    private void collectSelectedNodes(CheckedTreeNode node, List<MenuTreeNodeData> selectedItems) {
        if (node.isChecked() && node.getUserObject() instanceof MenuTreeNodeData) {
            selectedItems.add((MenuTreeNodeData) node.getUserObject());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                collectSelectedNodes((CheckedTreeNode) node.getChildAt(i), selectedItems);
            }
        }
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
                return menuItem.getExtensibleModel().getData().getMenuName();
            } else if (!isMenu && functionItem != null && functionItem.getExtensibleModel() != null && 
                      functionItem.getExtensibleModel().getData() != null) {
                String name = functionItem.getExtensibleModel().getData().getSubTransName();
                String code = functionItem.getExtensibleModel().getData().getSubTransCode();
                return name + " (" + code + ")";
            }
            return "未知项目";
        }
    }

    /**
     * 自定义树节点渲染器
     */
    private static class MenuTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        @Override
        public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, 
                                    boolean leaf, int row, boolean hasFocus) {
            if (value instanceof CheckedTreeNode) {
                CheckedTreeNode node = (CheckedTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof MenuTreeNodeData) {
                    MenuTreeNodeData data = (MenuTreeNodeData) userObject;
                    if (data.isMenu()) {
                        // 菜单节点 - 使用文件夹图标
                        getTextRenderer().setIcon(expanded ? AllIcons.Nodes.Folder : AllIcons.Nodes.Folder);
                        getTextRenderer().append(data.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else {
                        // 功能号节点 - 使用方法图标
                        getTextRenderer().setIcon(AllIcons.Nodes.Method);
                        getTextRenderer().append(data.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                } else {
                    // 根节点
                    getTextRenderer().setIcon(AllIcons.Nodes.Project);
                    getTextRenderer().append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        }
    }
}

