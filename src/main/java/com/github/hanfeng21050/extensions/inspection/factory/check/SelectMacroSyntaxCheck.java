package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class SelectMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String SELECT_TEMPLATE = "[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]";
    private static final String SELECT_LIST_TEMPLATE = "[selectList][sql][查询字段][条件语句][自定义动态条件][分组排序语句]";
    private static final String SELECT_DOC =
            "宏定义说明：\n" +
                    "1. [sql] - 必填参数\n" +
                    "   - 基本SQL查询语句\n" +
                    "   - 可以是简单查询或复杂的联表/子查询\n" +
                    "\n" +
                    "2. [查询字段] - 可选参数\n" +
                    "   - 简单SQL可不填\n" +
                    "   - 联表查询或子查询等复杂SQL需要填写\n" +
                    "\n" +
                    "3. [条件语句] - 可选参数\n" +
                    "   - 固定条件语句（不含where关键字）\n" +
                    "   - 作为必要的查询条件\n" +
                    "\n" +
                    "4. [自定义动态条件] - 可选参数\n" +
                    "   - 动态拼接的条件语句\n" +
                    "   - 仅在变量值非null时拼接\n" +
                    "   - 支持 /*#OR*/ 标记来替换默认的 AND 连接符\n" +
                    "\n" +
                    "5. [分组排序语句] - 可选参数\n" +
                    "   - 用于 ORDER BY、GROUP BY 等语句\n" +
                    "   - 使用动态条件时必须在此处指定排序/分组\n" +
                    "\n" +
                    "特殊用法：\n" +
                    "- 别名生成：使用 \"<A>\" 宏标记\n" +
                    "- OR条件：使用 \"/*#OR*/\" 标记\n" +
                    "\n" +
                    "示例：\n" +
                    "1. 基本查询：\n" +
                    "   @JRESMacro(\"[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = 1]\")\n" +
                    "\n" +
                    "2. 子查询：\n" +
                    "   @JRESMacro(\"[select][select * from (select client_id, exchange_type, current_balance from opt_fundreal where client_id = :client_id)][client_id, exchange_type, current_balance]\")\n" +
                    "\n" +
                    "3. 纯动态条件：\n" +
                    "   @JRESMacro(\"[select][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, branch_no = :branch_no]\")\n" +
                    "\n" +
                    "4. 混合条件：\n" +
                    "   @JRESMacro(\"[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account]\")\n" +
                    "\n" +
                    "5. 带排序：\n" +
                    "   @JRESMacro(\"[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account][order by init_date]\")\n" +
                    "\n" +
                    "6. 别名方式：\n" +
                    "   @JRESMacro(\"<A>[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = 1]\")\n" +
                    "\n" +
                    "7. OR条件：\n" +
                    "   @JRESMacro(\"[select][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, /*#OR*/branch_no = :branch_no]\")\n";

    private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":\\w+");
    private static final Pattern OR_CONDITION_PATTERN = Pattern.compile("/\\*#OR\\*/");

    private final String macroName;

    public SelectMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        if (params.size() < 2) {
            return generateError("错误：必须提供 [sql] 参数",
                    getTemplate(), SELECT_DOC);
        }

        String template = getTemplate();

        // 检查SQL语句（必填）
        String sqlParam = handleException(() -> params.get(1));
        if (StringUtils.isBlank(sqlParam)) {
            return generateError("错误：[sql] 参数不能为空", template, SELECT_DOC);
        }

        String error = validateSqlStatement(sqlParam);
        if (error != null) {
            return generateError("错误：SQL语句无效 - " + error, template, SELECT_DOC);
        }

        // 检查查询字段（可选）
        if (params.size() > 2) {
            String fieldsParam = handleException(() -> params.get(2));
            if (StringUtils.isNotBlank(fieldsParam)) {
                error = validateQueryFields(fieldsParam);
                if (error != null) {
                    return generateError("错误：查询字段无效 - " + error, template, SELECT_DOC);
                }
            }
        }

        // 检查固定条件语句（可选）
        if (params.size() > 3) {
            String conditionParam = handleException(() -> params.get(3));
            if (StringUtils.isNotBlank(conditionParam)) {
                error = validateWhereClause(conditionParam);
                if (error != null) {
                    return generateError("错误：固定条件语句无效 - " + error, template, SELECT_DOC);
                }
            }
        }

        // 检查动态条件（可选）
        if (params.size() > 4) {
            String dynamicConditionParam = handleException(() -> params.get(4));
            if (StringUtils.isNotBlank(dynamicConditionParam)) {
                error = validateDynamicConditions(dynamicConditionParam);
                if (error != null) {
                    return generateError("错误：动态条件无效 - " + error, template, SELECT_DOC);
                }
            }
        }

        // 检查分组排序语句（可选）
        if (params.size() > 5) {
            String orderByParam = handleException(() -> params.get(5));
            if (StringUtils.isNotBlank(orderByParam)) {
                error = validateOrderByClause(orderByParam);
                if (error != null) {
                    return generateError("错误：分组排序语句无效 - " + error, template, SELECT_DOC);
                }
            }
        }

        return null;
    }

    private String getTemplate() {
        return "select".equals(macroName) ? SELECT_TEMPLATE : SELECT_LIST_TEMPLATE;
    }

    private String validateSqlStatement(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String validateQueryFields(String fields) {
        try {
            CCJSqlParserUtil.parse("SELECT " + fields + " FROM dual");
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String validateWhereClause(String condition) {
        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + condition);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String validateDynamicConditions(String conditions) {
        // 处理 OR 条件标记
        String processedConditions = conditions;
        if (OR_CONDITION_PATTERN.matcher(conditions).find()) {
            processedConditions = conditions.replaceAll("/\\*#OR\\*/", "OR");
        }

        // 将逗号分隔的条件转换为 AND 连接
        processedConditions = processedConditions.replace(",", " AND ");

        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + processedConditions);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String validateOrderByClause(String clause) {
        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual " + clause);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
