package com.github.hanfeng21050.dialog.export;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.tree.DefaultMutableTreeNode;

public class FileTreeNode extends DefaultMutableTreeNode {
    private final VirtualFile file;
    private final boolean isFile;

    public FileTreeNode(VirtualFile file) {
        this(file, true);
    }

    public FileTreeNode(VirtualFile file, boolean isFile) {
        super(file.getName());
        this.file = file;
        this.isFile = isFile;
    }

    public VirtualFile getFile() {
        return file;
    }

    public boolean isFileNode() {
        return isFile;
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
