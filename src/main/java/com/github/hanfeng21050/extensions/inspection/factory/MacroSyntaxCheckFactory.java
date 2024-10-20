package com.github.hanfeng21050.extensions.inspection.factory;

import com.github.hanfeng21050.extensions.inspection.factory.check.*;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 11:20
 */
public class MacroSyntaxCheckFactory {

    public static SyntaxChecker getChecker(String macroName, String value) {
        if ("exists".equalsIgnoreCase(macroName) || "delete".equalsIgnoreCase(macroName)) {
            return new ExistsMacroSyntaxCheck(macroName, value);
        } else if ("insert".equalsIgnoreCase(macroName) || "batchInsert".equalsIgnoreCase(macroName) || "batchDelete".equalsIgnoreCase(macroName) || "getSerialNo".equalsIgnoreCase(macroName) || "getPrefetchSeq".equalsIgnoreCase(macroName)) {
            return new InsertMacroSyntaxCheck(macroName, value);
        } else if ("insertSelect".equalsIgnoreCase(macroName)) {
            return new insertSelectMacroSyntaxCheck(value);
        } else if ("select".equalsIgnoreCase(macroName) || "selectList".equalsIgnoreCase(macroName)) {
            return new SelectMacroSyntaxCheck(macroName, value);
        } else if ("selectPage".equalsIgnoreCase(macroName)) {
            return new SelectPageMacroSyntaxCheck(value);
        } else if ("selectRowNum".equalsIgnoreCase(macroName)) {
            return new SelectRowNumMacroSyntaxCheck(value);
        } else if ("update".equalsIgnoreCase(macroName)) {
            return new UpdateMacroSyntaxCheck(value);
        } else if ("getSequence".equalsIgnoreCase(macroName)) {
            return new GetSequenceMacroSyntaxCheck(value);
        } else if ("truncate".equalsIgnoreCase(macroName)) {
            return new TruncateMacroSyntaxCheck(value);
        }
        return null;
    }
}
