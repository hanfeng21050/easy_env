package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class UpdateMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String TEMPLATE = "[update][表名][更新字段][条件语句][更新语句]";
    private static final String DOC = """
            宏定义说明：
            1. 简单模式（推荐）：
               - 只使用第一个参数
               - 参数为完整的 UPDATE SQL 语句
               - 支持所有复杂场景
               示例：@JRESMacro("[update][update act_join_stockacct set socialral_type = case when socialral_type in ('0','1') then '2' end,client_name = :client_name where id_no=:id_no]")
            
            2. 键值对模式：
               - [表名] - 数据库表名
               - [更新字段] - 键值对格式，用于简单赋值
               - [条件语句] - WHERE 条件
               示例：@JRESMacro("[update][client][branch_no=:branch_no,client_id][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            
            3. 复杂更新模式：
               - [表名] - 数据库表名
               - [更新字段] - 简单键值对
               - [条件语句] - WHERE 条件
               - [更新语句] - 复杂的 SET 子句
               示例：@JRESMacro("[update][prd_busin_entrust][init_date = :init_date,entrust_no=:report_no][init_date = :init_date and entrust_no=:report_no and otc_entrust_status <> '2'][otc_entrust_status = case when otc_entrust_status in ('0', '1') then '2' else otc_entrust_status end]")
            """;

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w*$");

    public UpdateMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        if (params.size() < 2) {
            return generateError("错误：参数不足", TEMPLATE, DOC);
        }

        String param1 = handleException(() -> params.get(1));
        if (StringUtils.isBlank(param1)) {
            return generateError("错误：第一个参数不能为空", TEMPLATE, DOC);
        }

        // 简单模式：完整的 UPDATE SQL
        if (param1.toLowerCase().trim().startsWith("update")) {
            if (params.size() > 2 && (StringUtils.isNotBlank(params.get(2)) ||
                    (params.size() > 3 && StringUtils.isNotBlank(params.get(3))) ||
                    (params.size() > 4 && StringUtils.isNotBlank(params.get(4))))) {
                return generateError("错误：使用完整SQL模式时，不能指定其他参数", TEMPLATE, DOC);
            }
            try {
                CCJSqlParserUtil.parse(param1);
                return null;
            } catch (Exception e) {
                return generateError("错误：SQL语法错误 - " + e.getMessage(), TEMPLATE, DOC);
            }
        }

        // 表名验证
        if (!TABLE_NAME_PATTERN.matcher(param1).matches()) {
            return generateError("错误：表名格式无效，只能包含字母、数字和下划线，且必须以字母开头", TEMPLATE, DOC);
        }

        // 更新字段验证（键值对模式）
        String updateFields = handleException(() -> params.get(2));
        if (StringUtils.isNotBlank(updateFields)) {
            try {
                Map<String, String> fieldMap = convertKVString(updateFields);
                List<String> updateList = new ArrayList<>();
                for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                    updateList.add(entry.getKey() + "=" + entry.getValue());
                }
                CCJSqlParserUtil.parse("UPDATE table SET " + String.join(",", updateList));
            } catch (Exception e) {
                return generateError("错误：更新字段格式错误 - " + e.getMessage(), TEMPLATE, DOC);
            }
        }

        // 条件语句验证
        String whereClause = handleException(() -> params.get(3));
        if (StringUtils.isNotBlank(whereClause)) {
            try {
                CCJSqlParserUtil.parse("SELECT * FROM table WHERE " + whereClause);
            } catch (Exception e) {
                return generateError("错误：条件语句语法错误 - " + e.getMessage(), TEMPLATE, DOC);
            }
        }

        // 复杂更新语句验证
        String complexUpdate = handleException(() -> params.get(4));
        if (StringUtils.isNotBlank(complexUpdate)) {
            try {
                CCJSqlParserUtil.parse("UPDATE table SET " + complexUpdate);
            } catch (Exception e) {
                return generateError("错误：更新语句语法错误 - " + e.getMessage(), TEMPLATE, DOC);
            }
        }

        return null;
    }
}
