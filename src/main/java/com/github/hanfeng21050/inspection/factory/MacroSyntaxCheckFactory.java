package com.github.hanfeng21050.inspection.factory;

import com.github.hanfeng21050.inspection.factory.check.ExistsMacroSyntaxCheck;
import com.github.hanfeng21050.inspection.factory.check.SelectMacroSyntaxCheck;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 11:20
 */
public class MacroSyntaxCheckFactory {

    public static SyntaxChecker getChecker(String macroType, String value) {
        if ("exists".equalsIgnoreCase(macroType)) {
            return new ExistsMacroSyntaxCheck(value);
        } else if ("select".equalsIgnoreCase(macroType) || "selectList".equalsIgnoreCase(macroType)) {
            return new SelectMacroSyntaxCheck(value);
        }
        return null;
    }
}
