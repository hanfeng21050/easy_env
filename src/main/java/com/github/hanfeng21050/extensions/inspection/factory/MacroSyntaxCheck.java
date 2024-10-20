package com.github.hanfeng21050.extensions.inspection.factory;

import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 10:59
 */
public class MacroSyntaxCheck {
    // 定义允许的关键字集合
    public static final Set<String> ALLOWED_KEYWORDS = new HashSet<>(Arrays.asList(
            "exists", "insert", "insertSelect", "batchInsert", "delete", "batchDelete", "update", "batchUpdate",
            "select", "selectList", "selectRowNum", "selectPage", "getSerialNo", "getSequence", "truncate",
            "getTableColumn", "selectDynaSql", "selectDynaRowNumSql", "selectDynaPageSql", "getPrefetchSeq"
    ));
    public String value;

    public MacroSyntaxCheck(String value) {
        this.value = preprocessValue(value);
    }

    public String syntaxCheck() {
        String errorInfo = formatCheck();

        String macroName = extractParts(value).get(0);
        if (!ALLOWED_KEYWORDS.contains(macroName)) {
            return "无法解析宏名称. " + macroName;
        }

        SyntaxChecker macroSyntaxCheck = MacroSyntaxCheckFactory.getChecker(macroName, value);
        if (StringUtils.isBlank(errorInfo)) {
            // 调用子类的syntaxCheck方法
            if (macroSyntaxCheck != null) {
                List<String> params = extractParts(value);
                errorInfo = macroSyntaxCheck.performSyntaxCheck(params);
            }
        }
        return errorInfo;
    }

    /**
     * @return {@link String }
     */
    public String formatCheck() {
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

    public String generateError(String message, String template, String doc) {
        return String.format("%s \n %s \n %s", message, template, doc);
    }

    public String generateError(String message, String errorInfo, String template, String temp) {
        return String.format("%s \n %s \n %s \n %s", message, errorInfo, template, temp);
    }

    protected List<String> extractParts(String input) {
        List<String> parts = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            parts.add(matcher.group(1));
        }
        return parts;
    }

    protected <T> T handleException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    protected Map<String, String> convertKVString(String input) {
        String[] split = input.split(",");
        Map<String, String> kvMap = new HashMap<>();
        for (String s : split) {
            if (s.contains("=")) {
                kvMap.put(s.substring(0, s.indexOf("=")), s.substring(s.indexOf("=") + 1));
            } else {
                kvMap.put(s, s);
            }
        }
        return kvMap;
    }

}

