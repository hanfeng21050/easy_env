package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class insertSelectMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String template = "[insertSelect][插入目标表][获取字段表][字段1=value1,字段2=value2...][条件语句]";
    String doc = """
            如果第三个参数不填，则插入到目标表的字段为目标表和源表的所有同名字段；非同名字段在第三个参数中通过键值对的方式指定，如下例中会把act_fund_account_jour表中的witness_flag字段的值插入到act_fund_account_control_jour表的handle_flag字段中；第四个参数条件语句需填写完整的条件语句
            @JRESMacro("[insertSelect][act_fund_account_control_jour][act_fund_account_jour][handle_flag=witness_flag][branch_no=:branch_no and op_entrust_way=:op_entrust_way]")
            """;
    public insertSelectMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // [插入目标表]
        String param1 = handleException(() -> params.get(1));
        // [获取字段表]
        String param2 = handleException(() -> params.get(2));
        // [字段1=value1,字段2=value2...]
        String param3 = handleException(() -> params.get(3));
        // 条件语句
        String param4 = handleException(() -> params.get(4));

        if ((StringUtils.isNotBlank(param1) && !param1.matches("\\w+")) || StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[插入目标表].", template, doc);
        }

        if ((StringUtils.isNotBlank(param2) && !param1.matches("\\w+")) || StringUtils.isBlank(param2)) {
            return generateError("错误：无法解析[获取字段表].", template, doc);
        }

        if (StringUtils.isNotBlank(param3)) {
            String[] split = param3.split(",");
            Map<String, String> kvMap = new HashMap<>();
            for (String s : split) {
                if (s.contains("=")) {
                    kvMap.put(s.substring(0, s.indexOf("=")).trim(), s.substring(s.indexOf("=") + 1).trim());
                } else {
                    kvMap.put(s.trim(), " ");
                }
            }

            String[] keys = kvMap.keySet().toArray(new String[0]);
            Arrays.sort(keys);

            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = kvMap.get(keys[i]);
            }

            String keyStr = String.join(",", keys);
            String valueStr = String.join(",", values);

            try {
                CCJSqlParserUtil.parse("insert into table (" + keyStr + ") select (" + valueStr + ") from table");
            } catch (Exception e) {
                return generateError("错误：无法解析[字段1=value1,字段2=value2...].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param4)) {
            try {
                CCJSqlParserUtil.parse("select * from table where " + param4);
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);
            }
        }


        return "";
    }
}
