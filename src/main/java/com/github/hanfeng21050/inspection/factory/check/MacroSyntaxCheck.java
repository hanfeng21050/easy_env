package com.github.hanfeng21050.inspection.factory.check;

import com.github.hanfeng21050.inspection.factory.MacroSyntaxCheckFactory;
import com.github.hanfeng21050.inspection.factory.SyntaxChecker;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 10:59
 */
public class MacroSyntaxCheck {
    public final String value;
    // 定义允许的关键字集合
    public static final Set<String> ALLOWED_KEYWORDS = new HashSet<>(Arrays.asList(
            "exists", "insert", "insertSelect", "batchInsert", "delete", "batchDelete", "update", "batchUpdate",
            "select", "selectList", "selectRowNum", "selectPage", "getSerialNo", "getSequence", "truncate",
            "getTableColumn", "selectDynaSql", "selectDynaRowNumSql", "selectDynaPageSql", "getPrefetchSeq"
    ));

    public MacroSyntaxCheck(String value) {
        this.value = preprocessValue(value);
    }

    public String syntaxCheck() {
        String errorInfo = formatCheck(value);

        String macroName = value.substring(1, value.length() - 1).split("]\\[")[0];
        if (!ALLOWED_KEYWORDS.contains(macroName)) {
            return "无法解析宏名称. " + macroName;
        }

        SyntaxChecker macroSyntaxCheck = MacroSyntaxCheckFactory.getChecker(macroName, value);
        if (StringUtils.isBlank(errorInfo)) {
            // 调用子类的syntaxCheck方法
            if (macroSyntaxCheck != null) {
                errorInfo = macroSyntaxCheck.performSyntaxCheck();
            }
        }
        return errorInfo;
    }

    public String formatCheck(String value) {
        if (!value.contains("[") && !value.contains("]")) {
            return "语法不正确, 正确写法请参考文档";
        }
        Stack<Character> stack = new Stack<>();
        for (char c : value.toCharArray()) {
            if (c == '[') {
                stack.push(c);
            } else if (c == ']') {
                if (stack.isEmpty()) {
                    return "缺失左括号 [";
                }
                stack.pop();
            }
        }
        return stack.isEmpty() ? "" : "缺失右括号 ]";
    }

    private String preprocessValue(String value) {
        return value.replaceAll("[\"\\+\\r\\n]+", "")
                .replaceAll("\\s+", " ")
                .replaceAll("] \\[", "][");
    }

    public String generateError(String message, String context, String template) {
        return message + "\n\n" + context + "\n\n" + template;
    }

    public String generateError(String message, String context, String errorInfo, String template) {
        return message + "\n\n" + context + "\n\n" + errorInfo + "\n\n" + template;
    }
}

