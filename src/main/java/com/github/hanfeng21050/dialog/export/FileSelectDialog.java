package com.github.hanfeng21050.dialog.export;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.SearchTextField;
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
import java.util.stream.Collectors;

/**
 * 文件选择对话框
 * 用于展示和选择.hepbiz文件
 */
public class FileSelectDialog extends DialogWrapper {
    // 当前项目
    private final Project project;
    // 树形控件，用于展示文件结构
    private final CheckboxTree tree;
    // 树的根节点
    private final CheckedTreeNode root;
    // 文件分组映射：第一层为studio-resources上级目录，剩余层级为interface-service下的目录结构
    private final Map<String, Map<String, List<VirtualFile>>> fileGroups;
    // 搜索文本框
    private final SearchTextField searchField;
    // 所有文件的集合
    private final Collection<VirtualFile> allFiles;

    public FileSelectDialog(Project project, Collection<VirtualFile> files) {
        super(project);
        setTitle("选择接口文件");
        this.project = project;
        this.allFiles = files;

        // 按目录分组文件
        fileGroups = new HashMap<>();
        for (VirtualFile file : files) {
            // 只处理.hepbiz文件
            if (!file.getName().endsWith(".hepbiz")) {
                continue;
            }

            // 查找studio-resources的上级目录和interface-service目录
            VirtualFile current = file.getParent();
            String firstLevel = null;
            List<String> subPaths = new ArrayList<>();
            VirtualFile interfaceServiceDir = null;
            VirtualFile studioResourcesParent = null;

            // 首先找到studio-resources的上级目录
            while (current != null) {
                if (current.getName().equals("studio-resources")) {
                    studioResourcesParent = current.getParent();
                    if (studioResourcesParent != null) {
                        firstLevel = studioResourcesParent.getName();
                        break;
                    }
                }
                current = current.getParent();
            }

            // 如果找到了studio-resources的上级目录，继续查找interface-service
            if (firstLevel != null) {
                current = file.getParent();
                while (current != null && !current.equals(studioResourcesParent)) {
                    if (current.getName().equals("interface-service")) {
                        interfaceServiceDir = current;
                        break;
                    }
                    current = current.getParent();
                }

                // 如果找到了interface-service目录，构建从interface-service到文件的路径
                if (interfaceServiceDir != null) {
                    current = file.getParent();
                    while (current != null && !current.equals(interfaceServiceDir)) {
                        subPaths.add(0, current.getName());  // 添加到列表开头
                        current = current.getParent();
                    }
                } else {
                    // 如果没找到interface-service目录，使用从studio-resources到文件的路径
                    current = file.getParent();
                    while (current != null && !current.equals(studioResourcesParent)) {
                        subPaths.add(0, current.getName());
                        current = current.getParent();
                    }
                }

                // 确保至少有一个子路径
                if (subPaths.isEmpty()) {
                    subPaths.add("default");
                }

                // 将文件添加到对应的分组中
                String key = String.join("/", subPaths);
                fileGroups.computeIfAbsent(firstLevel, k -> new HashMap<>())
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(file);
            } else {
                // 如果没有找到studio-resources目录，放入未分类
                firstLevel = "未分类";
                if (file.getParent() != null) {
                    subPaths.add(file.getParent().getName());
                } else {
                    subPaths.add("default");
                }
                String key = String.join("/", subPaths);
                fileGroups.computeIfAbsent(firstLevel, k -> new HashMap<>())
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(file);
            }
        }

        // 创建搜索框
        searchField = new SearchTextField();
        searchField.addDocumentListener(new com.intellij.ui.DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
                filterTree(searchField.getText());
            }
        });

        // 创建树的根节点
        root = new CheckedTreeNode(null);
        root.setChecked(false);

        // 创建树控件
        tree = new CheckboxTree(
                new CheckboxTree.CheckboxTreeCellRenderer() {
                    @Override
                    public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                        if (!(value instanceof CheckedTreeNode node)) return;

                        if (node.getUserObject() instanceof FileTreeNode fileNode) {
                            // 文件节点显示
                            getTextRenderer().append(fileNode.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                            getTextRenderer().setIcon(AllIcons.FileTypes.Text);
                        } else if (node == root) {
                            // 根节点显示
                            getTextRenderer().append("接口文件", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                            getTextRenderer().setIcon(AllIcons.Nodes.Folder);
                        } else {
                            // 目录节点显示
                            getTextRenderer().append(node.getUserObject().toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                            getTextRenderer().setIcon(AllIcons.Nodes.Folder);
                        }
                    }
                },
                root
        );

        // 初始化树结构
        buildTreeStructure();
        init();
    }

    private void buildTreeStructure() {
        root.removeAllChildren();

        // 对第一层目录进行排序
        List<String> sortedFirstLevel = new ArrayList<>(fileGroups.keySet());
        Collections.sort(sortedFirstLevel);

        for (String firstLevel : sortedFirstLevel) {
            CheckedTreeNode firstLevelNode = new CheckedTreeNode(firstLevel);
            firstLevelNode.setChecked(false);
            root.add(firstLevelNode);

            Map<String, List<VirtualFile>> subPathMap = fileGroups.get(firstLevel);
            List<String> sortedSubPaths = new ArrayList<>(subPathMap.keySet());
            Collections.sort(sortedSubPaths);

            for (String subPath : sortedSubPaths) {
                // 创建子路径的节点层级
                String[] pathParts = subPath.split("/");
                CheckedTreeNode currentNode = firstLevelNode;

                // 为每个路径部分创建节点
                for (int i = 0; i < pathParts.length; i++) {
                    String pathPart = pathParts[i];
                    CheckedTreeNode newNode = null;

                    // 查找是否已存在相同名称的节点
                    for (int j = 0; j < currentNode.getChildCount(); j++) {
                        CheckedTreeNode child = (CheckedTreeNode) currentNode.getChildAt(j);
                        if (child.getUserObject().toString().equals(pathPart)) {
                            newNode = child;
                            break;
                        }
                    }

                    // 如果节点不存在，创建新节点
                    if (newNode == null) {
                        newNode = new CheckedTreeNode(pathPart);
                        newNode.setChecked(false);
                        currentNode.add(newNode);
                    }

                    currentNode = newNode;
                }

                // 在最后一层添加文件
                List<VirtualFile> files = subPathMap.get(subPath);
                files.sort(Comparator.comparing(VirtualFile::getName));
                for (VirtualFile file : files) {
                    CheckedTreeNode fileNode = new CheckedTreeNode(new FileTreeNode(file));
                    fileNode.setChecked(false);
                    currentNode.add(fileNode);
                }
            }
        }

        ((DefaultTreeModel) tree.getModel()).reload();
        tree.expandPath(new TreePath(root.getPath()));
    }

    private void filterTree(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            buildTreeStructure();
            return;
        }

        searchText = searchText.toLowerCase();
        root.removeAllChildren();

        for (Map.Entry<String, Map<String, List<VirtualFile>>> firstLevelEntry : fileGroups.entrySet()) {
            String firstLevel = firstLevelEntry.getKey();
            Map<String, List<VirtualFile>> subPathMap = firstLevelEntry.getValue();
            boolean hasMatchInFirstLevel = false;
            CheckedTreeNode firstLevelNode = new CheckedTreeNode(firstLevel);
            firstLevelNode.setChecked(false);

            for (Map.Entry<String, List<VirtualFile>> subPathEntry : subPathMap.entrySet()) {
                String subPath = subPathEntry.getKey();
                List<VirtualFile> files = subPathEntry.getValue();

                // 过滤匹配的文件
                String finalSearchText = searchText;
                List<VirtualFile> matchedFiles = files.stream()
                        .filter(file -> file.getName().toLowerCase().contains(finalSearchText) ||
                                firstLevel.toLowerCase().contains(finalSearchText) ||
                                subPath.toLowerCase().contains(finalSearchText))
                        .collect(Collectors.toList());

                if (!matchedFiles.isEmpty()) {
                    hasMatchInFirstLevel = true;
                    String[] pathParts = subPath.split("/");
                    CheckedTreeNode currentNode = firstLevelNode;

                    // 创建路径节点
                    for (String pathPart : pathParts) {
                        CheckedTreeNode newNode = new CheckedTreeNode(pathPart);
                        newNode.setChecked(false);
                        currentNode.add(newNode);
                        currentNode = newNode;
                    }

                    // 添加匹配的文件
                    matchedFiles.sort(Comparator.comparing(VirtualFile::getName));
                    for (VirtualFile file : matchedFiles) {
                        CheckedTreeNode fileNode = new CheckedTreeNode(new FileTreeNode(file));
                        fileNode.setChecked(false);
                        currentNode.add(fileNode);
                    }
                }
            }

            if (hasMatchInFirstLevel) {
                root.add(firstLevelNode);
            }
        }

        ((DefaultTreeModel) tree.getModel()).reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 500));

        // 添加搜索框
        panel.add(searchField, BorderLayout.NORTH);

        // 添加树控件
        panel.add(new JBScrollPane(tree), BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected void doOKAction() {
        List<VirtualFile> selectedFiles = getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Messages.showWarningDialog(
                    "请至少选择一个文件",
                    "警告"
            );
            return;
        }
        super.doOKAction();
    }

    public List<VirtualFile> getSelectedFiles() {
        List<VirtualFile> selectedFiles = new ArrayList<>();
        collectSelectedFiles(root, selectedFiles);
        return selectedFiles;
    }

    private void collectSelectedFiles(CheckedTreeNode node, List<VirtualFile> selectedFiles) {
        if (node.getUserObject() instanceof FileTreeNode && node.isChecked()) {
            selectedFiles.add(((FileTreeNode) node.getUserObject()).getFile());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (node.getChildAt(i) instanceof CheckedTreeNode) {
                collectSelectedFiles((CheckedTreeNode) node.getChildAt(i), selectedFiles);
            }
        }
    }
}
