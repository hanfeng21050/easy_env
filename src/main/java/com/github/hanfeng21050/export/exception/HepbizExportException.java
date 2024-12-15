package com.github.hanfeng21050.export.exception;

/**
 * Hepbiz导出异常
 */
public class HepbizExportException extends Exception {

    public HepbizExportException(String message) {
        super(message);
    }

    public HepbizExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
