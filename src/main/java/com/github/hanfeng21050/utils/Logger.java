package com.github.hanfeng21050.utils;

import com.github.hanfeng21050.extensions.ToolWindow.EasyEnvLogWindow;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;

public class Logger {
    private static EasyEnvLogWindow easyEnvLogWindowInstance;
    private static ToolWindow toolWindow;

    public static void setViewBarsInstance(EasyEnvLogWindow instance, ToolWindow window) {
        easyEnvLogWindowInstance = instance;
        toolWindow = window;
    }

    public static void info(String message) {
        log(formatMessage(message), LogLevel.INFO);
    }

    public static void warn(String message) {
        log(formatMessage(message), LogLevel.WARN);
    }

    public static void error(String message) {
        log(formatMessage(message), LogLevel.ERROR);
    }

    public static void error(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder(message);
        if (throwable != null) {
            sb.append("\n").append(throwable.getClass().getName())
                    .append(": ").append(throwable.getMessage());

            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                sb.append("\n    at ").append(element.toString());
            }
        }
        log(formatMessage(sb.toString()), LogLevel.ERROR);
    }

    private static String formatMessage(String message) {
        if (message == null) return "";

        // 只需要将反斜杠转换为正斜杠
        return message.replace('\\', '/');
    }

    private static void log(String message, LogLevel level) {
        if (easyEnvLogWindowInstance != null && toolWindow != null) {
            easyEnvLogWindowInstance.appendLog(message, level);
            if (!toolWindow.isVisible()) {
                SwingUtilities.invokeLater(() -> {
                    if (!toolWindow.isVisible()) {
                        toolWindow.activate(null);
                    }
                });
            }
        }
    }
}