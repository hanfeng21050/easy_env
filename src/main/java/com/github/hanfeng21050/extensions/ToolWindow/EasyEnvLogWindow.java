package com.github.hanfeng21050.extensions.ToolWindow;

import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasyEnvLogWindow extends SimpleToolWindowPanel {
    private static final int MAX_LINES = 1000;
    private static final Pattern FILE_PATTERN = Pattern.compile("([A-Za-z]:/(?:[^\\s/]+/)*[^\\s/]+(?:\\.[^\\s/]+)?)");
    private final JTextPane logPane;
    private final JBScrollPane scrollPane;
    private final StyledDocument document;
    private final Project project;
    private final ToolWindow toolWindow;

    public EasyEnvLogWindow(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        super(true, true);
        this.project = project;
        this.toolWindow = toolWindow;

        logPane = new JTextPane();
        logPane.setEditable(false);
        document = logPane.getStyledDocument();

        // 设置字体
        Font font = new Font("Monospaced", Font.PLAIN, JBUI.scaleFontSize(12));
        logPane.setFont(font);

        // 添加鼠标监听器
        setupMouseListeners();

        // 创建滚动面板
        scrollPane = new JBScrollPane(logPane);
        setContent(scrollPane);
    }

    private void setupMouseListeners() {
        logPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isControlDown() && SwingUtilities.isLeftMouseButton(e)) {
                    int offset = logPane.viewToModel2D(e.getPoint());
                    String filePath = findFilePathAtOffset(offset);
                    if (filePath != null) {
                        openFile(filePath);
                    }
                }
            }
        });

        logPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int offset = logPane.viewToModel2D(e.getPoint());
                String filePath = findFilePathAtOffset(offset);
                if (filePath != null && e.isControlDown()) {
                    logPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    logPane.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
    }

    private String findFilePathAtOffset(int offset) {
        try {
            String text = document.getText(0, document.getLength());
            Matcher matcher = FILE_PATTERN.matcher(text);
            while (matcher.find()) {
                if (matcher.start() <= offset && offset <= matcher.end()) {
                    String path = matcher.group();
                    if (new File(path).exists()) {
                        return path;
                    }
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void openFile(String filePath) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile != null && virtualFile.exists()) {
            new OpenFileDescriptor(project, virtualFile).navigate(true);
        }
    }

    public void appendLog(String message, LogLevel level) {
        if (SwingUtilities.isEventDispatchThread()) {
            appendLogImpl(message, level);
        } else {
            SwingUtilities.invokeLater(() -> appendLogImpl(message, level));
        }
    }

    private void appendLogImpl(String message, LogLevel level) {
        try {
            // 创建基本样式
            SimpleAttributeSet baseStyle = new SimpleAttributeSet();
            switch (level) {
                case ERROR:
                    StyleConstants.setForeground(baseStyle, new Color(255, 0, 0)); // 红色
                    break;
                case WARN:
                    StyleConstants.setForeground(baseStyle, new Color(255, 165, 0)); // 橙色
                    break;
                case INFO:
                    StyleConstants.setForeground(baseStyle, new Color(0, 0, 0)); // 黑色
                    break;
            }

            // 格式化时间戳和日志级别
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String prefix = String.format("[%s] [%s] ", timestamp, level);
            document.insertString(document.getLength(), prefix, baseStyle);

            // 处理消息部分，查找并高亮文件路径
            Matcher matcher = FILE_PATTERN.matcher(message);
            int lastEnd = 0;

            while (matcher.find()) {
                // 添加路径前的普通文本
                document.insertString(document.getLength(),
                        message.substring(lastEnd, matcher.start()), baseStyle);

                // 获取匹配的文件路径
                String filePath = matcher.group();

                // 为文件路径创建特殊样式
                SimpleAttributeSet pathStyle = new SimpleAttributeSet(baseStyle);
                StyleConstants.setUnderline(pathStyle, true);
                StyleConstants.setForeground(pathStyle, new Color(0, 0, 255)); // 蓝色

                // 添加带样式的文件路径
                document.insertString(document.getLength(), filePath, pathStyle);

                lastEnd = matcher.end();
            }

            // 添加剩余的文本
            if (lastEnd < message.length()) {
                document.insertString(document.getLength(),
                        message.substring(lastEnd), baseStyle);
            }

            // 添加换行符
            document.insertString(document.getLength(), "\n", baseStyle);

            // 自动滚动到底部
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

            // 如果超过最大行数，删除旧的行
            if (document.getDefaultRootElement().getElementCount() > MAX_LINES) {
                removeOldestLines(document.getDefaultRootElement().getElementCount() - MAX_LINES);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void removeOldestLines(int count) {
        try {
            int end = document.getDefaultRootElement().getElement(count - 1).getEndOffset();
            document.remove(0, end);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
