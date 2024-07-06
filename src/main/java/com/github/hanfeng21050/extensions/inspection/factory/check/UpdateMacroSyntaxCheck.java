package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String template = "[update][表名][更新字段][条件语句][更新语句]";
    String doc = """
            只写第一个参数时，参数含义会变为完整的updateSQL语句，可支持所有场景，推荐使用这种方式，示例见例1；第二个参数为可拼接的更新字段键值对，只能处理简单的赋值场景（比如简单的键值对赋值或使用单参数的函数），见例2；第四个参数为完整的更新语句，用于处理复杂的赋值场景（比如使用多参数的函数），见例3
            例1: @JRESMacro("[update][update act_join_stockacct set socialral_type = case when socialral_type in ('0','1') then '2' end,client_name = :client_name where id_no=:id_no]")
            例2: @JRESMacro("[update][client][branch_no=:branch_no,client_id][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            例3: @JRESMacro("[update][prd_busin_entrust][init_date = :init_date,entrust_no=:report_no][init_date = :init_date and entrust_no=:report_no and otc_entrust_status <> '2'][otc_entrust_status = case when otc_entrust_status in ('0', '1') then '2' else otc_entrust_status end]")
            """;
    public UpdateMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // [表名]
        String param1 = handleException(() -> params.get(1));
        // [更新字段]
        String param2 = handleException(() -> params.get(2));
        // [条件语句]
        String param3 = handleException(() -> params.get(3));
        // [更新语句]
        String param4 = handleException(() -> params.get(4));


        if (StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[表名].", template, doc);
        } else if (param1.contains("update") && StringUtils.isBlank(param2) && StringUtils.isBlank(param3) && StringUtils.isBlank(param4)) {
            try {
                CCJSqlParserUtil.parse(param1);
            } catch (Exception e) {
                return generateError("错误：无法解析[sql].", e.getMessage(), template, doc);
            }
        } else if (!param1.matches("\\w+")) {
            return generateError("错误：无法解析[表名].", template, doc);
        }

        if (StringUtils.isNotBlank(param2)) {
            Map<String, String> stringStringMap = convertKVString(param2);
            List<String> updateFields = new ArrayList();
            for (String s : stringStringMap.keySet()) {
                updateFields.add(s + "=" + stringStringMap.get(s));
            }

            try {
                CCJSqlParserUtil.parse("update table set " + String.join(",", updateFields));
            } catch (Exception e) {
                return generateError("错误：无法解析[更新字段].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param3)) {
            try {
                CCJSqlParserUtil.parse("select * from table where " + param3);
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param4)) {
            try {
                CCJSqlParserUtil.parse("update table set " + param4);
            } catch (Exception e) {
                return generateError("错误：无法解析[更新语句].", e.getMessage(), template, doc);
            }
        }

        return "";
    }
}
