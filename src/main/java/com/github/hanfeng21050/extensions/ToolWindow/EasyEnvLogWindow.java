package com.github.hanfeng21050.extensions.ToolWindow;

import com.github.hanfeng21050.utils.EasyIcons;
import com.github.hanfeng21050.utils.LogLevel;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBColor;
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

        // 初始化日志面板
        logPane = new JTextPane();
        logPane.setEditable(false);
        document = logPane.getStyledDocument();

        // 设置字体
        Font font = new Font("Monospaced", Font.PLAIN, JBUI.scaleFontSize(12));
        logPane.setFont(font);

        // 初始化滚动面板
        scrollPane = new JBScrollPane(logPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 初始化颜色
        updateColors();

        // 添加主题切换监听器
        project.getMessageBus().connect().subscribe(
                LafManagerListener.TOPIC,
                source -> SwingUtilities.invokeLater(() -> {
                    updateColors();
                    refreshAllText();
                })
        );

        // 添加编辑器主题切换监听器
        project.getMessageBus().connect().subscribe(
                EditorColorsManager.TOPIC,
                scheme -> SwingUtilities.invokeLater(() -> {
                    updateColors();
                    refreshAllText();
                })
        );

        // 添加鼠标监听器
        setupMouseListeners();

        // 设置工具栏
        setupToolbar();

        // 将滚动面板添加到主面板
        setContent(scrollPane);
    }

    /**
     * 更新所有颜色以匹配当前主题
     */
    private void updateColors() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        // 设置背景色
        Color backgroundColor = scheme.getDefaultBackground();
        logPane.setBackground(backgroundColor);
        scrollPane.setBackground(backgroundColor);
        scrollPane.getViewport().setBackground(backgroundColor);

        // 设置默认文本颜色
        Color foregroundColor = scheme.getDefaultForeground();
        logPane.setForeground(foregroundColor);

        // 刷新UI
        logPane.repaint();
        scrollPane.repaint();
    }

    /**
     * 获取指定日志级别在当前主题下的颜色
     */
    private Color getColorForLevel(LogLevel level) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        boolean isDarkTheme = EditorColorsManager.getInstance().isDarkEditor();

        return switch (level) {
            case INFO -> scheme.getDefaultForeground();
            case ERROR -> new JBColor(new Color(200, 0, 0), new Color(255, 100, 100));
            case WARN -> new JBColor(new Color(200, 130, 0), new Color(255, 200, 100));
        };
    }

    private Color getLinkColor() {
        boolean isDarkTheme = EditorColorsManager.getInstance().isDarkEditor();
        return new JBColor(
                new Color(0, 102, 204),  // Light theme - IntelliJ default link color
                new Color(104, 159, 220)  // Dark theme - IntelliJ default link color
        );
    }

    /**
     * 添加日志
     */
    public void appendLog(String message, LogLevel level) {
        SwingUtilities.invokeLater(() -> {
            // 添加时间戳和日志级别
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = String.format("[%s] [%s] %s%n", timestamp, level, message);

            // 使用appendLogImpl来处理文本，这样可以正确处理文件路径
            appendLogImpl(logEntry, level);

            // 限制行数
            limitLines();

            // 滚动到底部
            logPane.setCaretPosition(document.getLength());
        });
    }

    private void appendLogImpl(String message, LogLevel level) {
        try {
            // 创建基本样式
            SimpleAttributeSet baseStyle = new SimpleAttributeSet();
            Color color = getColorForLevel(level);
            StyleConstants.setForeground(baseStyle, color);

            // 处理消息部分，查找并高亮文件路径
            Matcher matcher = FILE_PATTERN.matcher(message);
            int lastEnd = 0;

            while (matcher.find()) {
                // 添加路径前的普通文本
                String beforePath = message.substring(lastEnd, matcher.start());
                document.insertString(document.getLength(), beforePath, baseStyle);

                // 获取匹配的文件路径
                String filePath = matcher.group();
                File file = new File(filePath);

                // 为文件路径创建特殊样式
                SimpleAttributeSet pathStyle = new SimpleAttributeSet(baseStyle);
                if (file.exists()) {
                    StyleConstants.setUnderline(pathStyle, true);
                    Color linkColor = getLinkColor();
                    StyleConstants.setForeground(pathStyle, linkColor);
                }

                // 添加带样式的文件路径
                document.insertString(document.getLength(), filePath, pathStyle);

                lastEnd = matcher.end();
            }

            // 添加剩余的文本
            if (lastEnd < message.length()) {
                document.insertString(document.getLength(),
                        message.substring(lastEnd), baseStyle);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
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

    private void setupToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.addAction(new AnAction("清除日志", "清除所有日志内容", EasyIcons.deleteIcon) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                clearLog();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(true);
            }
        });

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("EasyEnvLog", actionGroup, true);
        toolbar.setTargetComponent(this);
        setToolbar(toolbar.getComponent());
    }

    private void limitLines() {
        if (document.getDefaultRootElement().getElementCount() > MAX_LINES) {
            removeOldestLines(document.getDefaultRootElement().getElementCount() - MAX_LINES);
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

    /**
     * 清除日志内容
     */
    private void clearLog() {
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private void refreshAllText() {
        try {
            String fullText = document.getText(0, document.getLength());
            document.remove(0, document.getLength());

            // 分析每一行的日志级别
            String[] lines = fullText.split("\n", -1);  // 使用 -1 保留空行
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.isEmpty() && i == lines.length - 1) continue; // 跳过最后一个空行

                // 从日志行中提取日志级别
                LogLevel level = LogLevel.INFO;  // 默认级别
                if (line.contains("[ERROR]")) {
                    level = LogLevel.ERROR;
                } else if (line.contains("[WARN]")) {
                    level = LogLevel.WARN;
                }

                // 不在最后一行时添加换行符
                if (i < lines.length - 1) {
                    line += "\n";
                }

                appendLogImpl(line, level);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
