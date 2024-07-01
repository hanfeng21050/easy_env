package com.github.hanfeng21050.inspection.factory.check;

import com.github.hanfeng21050.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 11:12
 */
public class ExistsMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {

    public ExistsMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck() {
        String[] split = value.substring(1, value.length() - 1).split("]\\[");

        if (split.length < 2) {
            return generateError("无法解析[表名].", value, "模板：[exists][表名][条件语句][自定义动态条件]");
        }

        if (!split[1].matches("\\w+")) {
            return generateError("无法解析[表名].", value, "模板：[exists][表名][条件语句][自定义动态条件]");
        }

        StringBuilder testSql = new StringBuilder("select * from " + split[1]);
        if (split.length > 2 && StringUtils.isNotBlank(split[2])) {
            try {
                testSql.append(" where ").append(split[2]);
                CCJSqlParserUtil.parse(testSql.toString());
            } catch (Exception e) {
                // SQL syntax is incorrect
                return generateError("无法解析[条件语句].", value,  e.getMessage(), "模板：[exists][表名][条件语句][自定义动态条件]");

            }
        }

        if (split.length > 3) {
            String[] conditions = split[3].split(",");

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
                return generateError("无法解析[自定义动态条件].", value,  e.getMessage(), "模板：[exists][表名][条件语句][自定义动态条件]");
            }
        }
        return null;
    }
}
