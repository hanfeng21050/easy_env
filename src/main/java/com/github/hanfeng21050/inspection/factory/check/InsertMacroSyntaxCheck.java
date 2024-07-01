package com.github.hanfeng21050.inspection.factory.check;

import com.github.hanfeng21050.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 20:19
 */
public class InsertMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    public InsertMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck() {
        String[] split = value.substring(1, value.length() - 1).split("]\\[");

        if (split.length < 2) {
            return generateError("无法解析[表名].", value, "模板：[insert][表名][参数列表]");
        }

        if (!split[1].matches("\\w+")) {
            return generateError("无法解析[表名].", value, "模板：[insert][表名][参数列表]");
        }

        if (split.length > 2) {
            if (StringUtils.isBlank(split[2])) {
                return generateError("无法解析[参数列表].", value, "参数列表不能为空", "模板：[insert][表名][参数列表]");
            } else {
                // todo
            }
        }


        return null;
    }
}
