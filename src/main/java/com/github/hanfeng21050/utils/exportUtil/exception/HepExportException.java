package com.github.hanfeng21050.utils.exportUtil.exception;

/**
 * Hepbiz导出异常
 */
public class HepExportException extends Exception {

    public HepExportException(String message) {
        super(message);
    }

    public HepExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
