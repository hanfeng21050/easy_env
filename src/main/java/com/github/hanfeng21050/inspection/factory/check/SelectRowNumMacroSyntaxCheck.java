package com.github.hanfeng21050.inspection.factory.check;

import com.github.hanfeng21050.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 21:57
 */
public class SelectRowNumMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    public SelectRowNumMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck() {
        String[] split = value.substring(1, value.length() - 1).split("]\\[");

        if (split.length < 2) {
            return generateError("无法解析[sql].", value, "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
        } else {
            StringBuilder testSql = new StringBuilder(split[2]);
            if (StringUtils.isBlank(testSql.toString())) {
                return generateError("无法解析[sql].", value, "sql必填", "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
            } else {
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[sql].", value, e.getMessage(), "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 2) {
                if(StringUtils.isBlank(split[3])) {
                    return generateError("无法解析[sql].", value, "请求行数不能为空", "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                } else {
                    String sql = "select * from table where " + split[3];
                    try {
                        CCJSqlParserUtil.parse(sql);
                    } catch (Exception e) {
                        // SQL syntax is incorrect
                        return generateError("无法解析[rownum=请求行数].", value, e.getMessage(), "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                    }
                }
            }

            if (split.length > 3 && StringUtils.isNotBlank(split[3])) {
                String fields = split[3];
                String pattern = "^[a-zA-Z0-9_]+(,[a-zA-Z0-9_]+)*$";
                boolean matches = Pattern.matches(pattern, fields.replaceAll("[\\s\\t]", ""));
                if (!matches) {
                    return generateError("无法解析[查询字段].", value, "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 4 && StringUtils.isNotBlank(split[4])) {
                String condition = split[3];
                testSql.append(" where ").append(condition);
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[条件语句].", value, e.getMessage(), "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }
            if (split.length > 5 && StringUtils.isNotBlank(split[5])) {
                String condition = split[5];
                testSql.append(" where ").append("condition");
                String[] conditions = split[5].split(",");

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
                    return generateError("无法解析[自定义动态条件].", value, e.getMessage(), "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }

            if (split.length > 6 && StringUtils.isNotBlank(split[6])) {
                String condition = split[6];
                testSql.append(" ").append(condition);
                try {
                    CCJSqlParserUtil.parse(testSql.toString());
                } catch (Exception e) {
                    // SQL syntax is incorrect
                    return generateError("无法解析[分组排序语句].", value, e.getMessage(), "模板：[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]");
                }
            }
        }
        return null;
    }
}
