package com.github.hanfeng21050.utils;

import com.github.hanfeng21050.extensions.ToolWindow.EasyEnvLogWindow;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static EasyEnvLogWindow easyEnvLogWindowInstance;
    private static ToolWindow toolWindow;


    public static void setViewBarsInstance(EasyEnvLogWindow instance, ToolWindow window) {
        easyEnvLogWindowInstance = instance;
        toolWindow = window;
    }

    public static void info(String message) {
        String timestamp = dateFormat.format(new Date());
        String logMessage = String.format("[%s] %s", timestamp, message);
        log(logMessage);
    }

    private static void log(String message) {
        if (easyEnvLogWindowInstance != null && toolWindow != null) {
            easyEnvLogWindowInstance.appendLog(message);
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