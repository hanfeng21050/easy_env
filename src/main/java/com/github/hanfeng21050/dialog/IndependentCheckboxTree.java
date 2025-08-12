package com.github.hanfeng21050.dialog;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 独立的复选框树 - 父子节点完全不联动
 */
public class IndependentCheckboxTree extends JTree {

    public IndependentCheckboxTree(TreeNode root) {
        super(root);
        initializeTree();
    }

    private void initializeTree() {
        // 设置自定义渲染器
        setCellRenderer(new IndependentCheckboxRenderer());

        // 隐藏根节点
        setRootVisible(false);
        setShowsRootHandles(true);

        // 添加鼠标监听器处理复选框点击
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 防止文本选择
                e.consume();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // 防止文本选择
                e.consume();
            }
        });

        // 重写processMouseEvent来完全控制鼠标行为
        setFocusable(true);
        setRequestFocusEnabled(true);

        // 禁用文本选择
        putClientProperty("JTree.lineStyle", "None");
        setToggleClickCount(0); // 禁用双击展开

        // 设置空的选择模型来禁用文本选择
        setSelectionModel(new DefaultTreeSelectionModel() {
            @Override
            public void setSelectionPath(TreePath path) {
                // 不做任何事情，禁用选择
            }

            @Override
            public void setSelectionPaths(TreePath[] paths) {
                // 不做任何事情，禁用选择
            }

            @Override
            public void addSelectionPath(TreePath path) {
                // 不做任何事情，禁用选择
            }

            @Override
            public void addSelectionPaths(TreePath[] paths) {
                // 不做任何事情，禁用选择
            }
        });

        // 禁用默认的键盘事件
        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
        getInputMap().put(KeyStroke.getKeyStroke("ctrl SPACE"), "none");
    }

    /**
     * 处理鼠标点击事件
     */
    private void handleMouseClick(MouseEvent e) {
        TreePath path = getPathForLocation(e.getX(), e.getY());
        if (path != null) {
            Object node = path.getLastPathComponent();
            if (node instanceof IndependentTreeNode) {
                Rectangle bounds = getPathBounds(path);
                if (bounds != null) {
                    // 检查是否点击在复选框区域（左侧约20像素）
                    boolean isCheckboxClick = e.getX() >= bounds.x && e.getX() <= bounds.x + 20;

                    if (isCheckboxClick) {
                        // 切换选中状态
                        IndependentTreeNode treeNode = (IndependentTreeNode) node;
                        treeNode.setChecked(!treeNode.isChecked());

                        // 重绘该节点
                        repaint(bounds);
                    } else {
                        // 点击在文本区域，展开/折叠节点但不选中文本
                        IndependentTreeNode treeNode = (IndependentTreeNode) node;
                        if (treeNode.getChildCount() > 0) {
                            if (isExpanded(path)) {
                                collapsePath(path);
                            } else {
                                expandPath(path);
                            }
                        }
                    }

                    // 阻止默认的选择行为
                    e.consume();
                }
            }
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        // 拦截所有鼠标事件，防止默认的选择行为
        if (e.getID() == MouseEvent.MOUSE_PRESSED ||
                e.getID() == MouseEvent.MOUSE_RELEASED ||
                e.getID() == MouseEvent.MOUSE_CLICKED) {

            handleMouseClick(e);
            return; // 不调用super，完全接管鼠标处理
        }

        // 其他鼠标事件正常处理
        super.processMouseEvent(e);
    }

    /**
     * 获取所有选中的节点
     */
    public List<IndependentTreeNode> getCheckedNodes() {
        List<IndependentTreeNode> checkedNodes = new ArrayList<>();
        collectCheckedNodes((IndependentTreeNode) getModel().getRoot(), checkedNodes);
        return checkedNodes;
    }

    /**
     * 递归收集选中的节点
     */
    private void collectCheckedNodes(IndependentTreeNode node, List<IndependentTreeNode> checkedNodes) {
        if (node.isChecked()) {
            checkedNodes.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            TreeNode child = node.getChildAt(i);
            if (child instanceof IndependentTreeNode) {
                collectCheckedNodes((IndependentTreeNode) child, checkedNodes);
            }
        }
    }

    /**
     * 设置节点的选中状态（不影响其他节点）
     */
    public void setNodeChecked(IndependentTreeNode node, boolean checked) {
        node.setChecked(checked);
        repaint();
    }

    /**
     * 展开前两层节点
     */
    public void expandTopLevels() {
        SwingUtilities.invokeLater(() -> {
            TreeNode root = (TreeNode) getModel().getRoot();

            // 展开第一层
            for (int i = 0; i < root.getChildCount(); i++) {
                TreeNode firstLevel = root.getChildAt(i);
                expandPath(new TreePath(new Object[]{root, firstLevel}));

                // 展开第二层（只展开菜单节点）
                for (int j = 0; j < firstLevel.getChildCount(); j++) {
                    TreeNode secondLevel = firstLevel.getChildAt(j);
                    if (secondLevel instanceof IndependentTreeNode) {
                        IndependentTreeNode node = (IndependentTreeNode) secondLevel;
                        Object userObject = node.getUserObject();
                        if (userObject instanceof MenuTreeDialog.MenuTreeNodeData) {
                            MenuTreeDialog.MenuTreeNodeData data = (MenuTreeDialog.MenuTreeNodeData) userObject;
                            if (data.isMenu()) {
                                expandPath(new TreePath(new Object[]{root, firstLevel, secondLevel}));
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 自定义树节点，包含独立的选中状态
     */
    public static class IndependentTreeNode extends DefaultMutableTreeNode {
        private boolean checked = false;

        public IndependentTreeNode(Object userObject) {
            super(userObject);
        }

        public boolean isChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    /**
     * 自定义渲染器
     */
    public static class IndependentCheckboxRenderer extends DefaultTreeCellRenderer {
        private final JCheckBox checkBox;
        private final JPanel panel;

        public IndependentCheckboxRenderer() {
            checkBox = new JCheckBox();
            panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);

            checkBox.setOpaque(false);
            setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {

            Component rendererComponent = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof IndependentTreeNode) {
                IndependentTreeNode node = (IndependentTreeNode) value;

                // 设置复选框状态
                checkBox.setSelected(node.isChecked());

                // 设置文本和图标
                Object userObject = node.getUserObject();
                if (userObject instanceof MenuTreeDialog.MenuTreeNodeData) {
                    MenuTreeDialog.MenuTreeNodeData data = (MenuTreeDialog.MenuTreeNodeData) userObject;
                    if (data.isMenu()) {
                        setIcon(AllIcons.Nodes.Folder);
                        setText(data.toString());
                        setForeground(SimpleTextAttributes.REGULAR_ATTRIBUTES.getFgColor());
                    } else {
                        setIcon(AllIcons.Nodes.Method);
                        setText(data.toString());
                        setForeground(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES.getFgColor());
                    }
                } else {
                    setIcon(AllIcons.Nodes.Project);
                    setText(userObject.toString());
                }

                // 创建包含复选框和标签的面板
                panel.removeAll();
                panel.add(checkBox, BorderLayout.WEST);
                panel.add(this, BorderLayout.CENTER);

                // 设置背景色
                if (selected) {
                    panel.setBackground(getBackgroundSelectionColor());
                    setBackgroundNonSelectionColor(getBackgroundSelectionColor());
                } else {
                    panel.setBackground(getBackgroundNonSelectionColor());
                }

                return panel;
            }

            return rendererComponent;
        }
    }
}