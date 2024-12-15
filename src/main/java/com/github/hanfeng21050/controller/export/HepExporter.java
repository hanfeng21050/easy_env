package com.github.hanfeng21050.controller.export;

import com.github.hanfeng21050.exception.HepExportException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * 通用导出接口
 */
public interface HepExporter {
    /**
     * 导出文档
     *
     * @param project 项目
     * @param files   文件列表
     * @return 导出的文件内容
     * @throws HepExportException 导出异常
     */
    String export(Project project, List<VirtualFile> files) throws HepExportException;

    /**
     * 获取导出器名称
     *
     * @return 导出器名称
     */
    String getName();

    /**
     * 获取文件扩展名
     *
     * @return 文件扩展名
     */
    String getFileExtension();

    /**
     * 获取文件类型描述
     *
     * @return 文件类型描述
     */
    String getFileTypeDescription();
}
