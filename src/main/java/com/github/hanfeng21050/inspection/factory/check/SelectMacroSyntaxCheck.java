package com.github.hanfeng21050.inspection.factory.check;

import com.github.hanfeng21050.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 13:17
 */
public class SelectMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    public SelectMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck() {
        String[] split = value.substring(1, value.length() - 1).split("]\\[");

        if (split.length < 2) {
            return generateError("无法解析[sql].", value, "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
        } else {
            StringBuilder testSql = new StringBuilder(split[1]);
            if (StringUtils.isBlank(testSql.toString())) {
                return generateError("无法解析[sql].", value, "sql必填", "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
            } else {
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[sql].", value, e.getMessage(), "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 2 && StringUtils.isNotBlank(split[2])) {
                String fields = split[2];
                String pattern = "^[a-zA-Z0-9_]+(,[a-zA-Z0-9_]+)*$";
                boolean matches = Pattern.matches(pattern, fields.replaceAll("[\\s\\t]", ""));
                if (!matches) {
                    return generateError("无法解析[查询字段].", value, "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 3 && StringUtils.isNotBlank(split[3])) {
                String condition = split[3];
                testSql.append(" where ").append(condition);
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[条件语句].", value, e.getMessage(), "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }
            if (split.length > 4 && StringUtils.isNotBlank(split[4])) {
                String condition = split[4];
                testSql.append(" where ").append("condition");
                String[] conditions = split[4].split(",");

                for (String s : conditions) {
                    if (testSql.toString().contains("where")) {
                        testSql.append(s.contains("/*#OR*/") ? " or " : " and ").append(s.replace("/*#OR*/", ""));
                    } else {
                        testSql.append(" where ").append(s);
                    }
                }

                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[自定义动态条件].", value, e.getMessage(), "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 5 && StringUtils.isNotBlank(split[5])) {
                String condition = split[5];
                testSql.append(" ").append(condition);
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[分组排序语句].", value, e.getMessage(), "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }
        }
        return null;
    }
}
