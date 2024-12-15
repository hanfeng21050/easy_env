package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SelectPageMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String TEMPLATE = "[selectPage][sql][pageNo=:pageNo,pageSize=:pageSize,rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]";
    private static final String DOC = """
            宏定义说明：
            1. [sql] - 必填参数
               - 基本SQL查询语句
               - 可以是简单查询或复杂的联表/子查询
            
            2. [分页参数] - 部分可选
               - rownum: 必填，指定请求行数
               - pageNo: 可选，页码
               - pageSize: 可选，每页大小
            
            3. [查询字段] - 可选参数
               - 简单SQL可不填
               - 联表查询或子查询需要填写返回字段
            
            4. [条件语句] - 可选参数
               - 固定条件语句（不含where关键字）
               - 作为必要的查询条件
            
            5. [自定义动态条件] - 可选参数
               - 动态拼接的条件语句
               - 仅在变量值非null时拼接
               - 支持 /*#OR*/ 标记来替换默认的 AND 连接符
            
            6. [分组排序语句] - 可选参数
               - 用于 ORDER BY、GROUP BY 等语句
               - 使用动态条件时必须在此处指定排序/分组
            
            特殊用法：
            - 别名生成：使用 "<A>" 宏标记
            - OR条件：使用 "/*#OR*/" 标记
            
            示例：
            1. 基本分页查询：
               @JRESMacro("[selectPage][select risk_level_old,init_date,corp_risk_level from elg_client_risk_calm][pageNo=:pageNo,pageSize=:pageSize,rownum=1000][][client_id = :client_id and branch_no > :branch_no]")
            
            2. 子查询分页：
               @JRESMacro("[selectPage][select * from (select client_id, exchange_type, current_balance from opt_fundreal where client_id = :client_id)][pageNo=:pageNo,pageSize=100,rownum=1000][client_id, exchange_type, current_balance]")
            
            3. 纯动态条件：
               @JRESMacro("[selectPage][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][][client_id =:client_id, branch_no = :branch_no]")
            
            4. 混合条件：
               @JRESMacro("[selectPage][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account]")
            
            5. 带排序：
               @JRESMacro("[selectPage][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account][order by init_date]")
            
            6. 别名方式：
               @JRESMacro("<A>[selectPage][select risk_level_old,init_date,corp_risk_level from elg_client_risk_calm][pageNo=:pageNo,pageSize=:pageSize,rownum=1000][][client_id = :client_id and branch_no > :branch_no]")
            
            7. OR条件：
               @JRESMacro("[selectPage][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][][client_id =:client_id, /*#OR*/branch_no = :branch_no]")
            """;

    private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":\\w+");
    private static final Pattern OR_CONDITION_PATTERN = Pattern.compile("/\\*#OR\\*/");
    private static final Pattern PAGE_PARAM_PATTERN = Pattern.compile("(pageNo|pageSize|rownum)\\s*=\\s*([^,\\s]+)");

    public SelectPageMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        if (params.size() < 2) {
            return generateError("错误：必须提供 [sql] 参数", TEMPLATE, DOC);
        }

        // 检查SQL语句（必填）
        String sqlParam = handleException(() -> params.get(1));
        if (StringUtils.isBlank(sqlParam)) {
            return generateError("错误：[sql] 参数不能为空", TEMPLATE, DOC);
        }

        String error = validateSqlStatement(sqlParam);
        if (error != null) {
            return generateError("错误：SQL语句无效 - " + error, TEMPLATE, DOC);
        }

        // 检查分页参数（rownum必填）
        if (params.size() > 2) {
            String pageParams = handleException(() -> params.get(2));
            error = validatePageParameters(pageParams);
            if (error != null) {
                return generateError("错误：分页参数无效 - " + error, TEMPLATE, DOC);
            }
        } else {
            return generateError("错误：必须提供分页参数 [rownum]", TEMPLATE, DOC);
        }

        // 检查查询字段（可选）
        if (params.size() > 3) {
            String fieldsParam = handleException(() -> params.get(3));
            if (StringUtils.isNotBlank(fieldsParam)) {
                error = validateQueryFields(fieldsParam);
                if (error != null) {
                    return generateError("错误：查询字段无效 - " + error, TEMPLATE, DOC);
                }
            }
        }

        // 检查固定条件语句（可选）
        if (params.size() > 4) {
            String conditionParam = handleException(() -> params.get(4));
            if (StringUtils.isNotBlank(conditionParam)) {
                error = validateWhereClause(conditionParam);
                if (error != null) {
                    return generateError("错误：固定条件语句无效 - " + error, TEMPLATE, DOC);
                }
            }
        }

        // 检查动态条件（可选）
        if (params.size() > 5) {
            String dynamicConditionParam = handleException(() -> params.get(5));
            if (StringUtils.isNotBlank(dynamicConditionParam)) {
                error = validateDynamicConditions(dynamicConditionParam);
                if (error != null) {
                    return generateError("错误：动态条件无效 - " + error, TEMPLATE, DOC);
                }
            }
        }

        // 检查分组排序语句（可选）
        if (params.size() > 6) {
            String orderByParam = handleException(() -> params.get(6));
            if (StringUtils.isNotBlank(orderByParam)) {
                error = validateOrderByClause(orderByParam);
                if (error != null) {
                    return generateError("错误：分组排序语句无效 - " + error, TEMPLATE, DOC);
                }
            }
        }

        return null;
    }

    private String validateSqlStatement(String sql) {
        try {
            CCJSqlParserUtil.parse(sql);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String validatePageParameters(String pageParams) {
        if (StringUtils.isBlank(pageParams)) {
            return "分页参数不能为空";
        }

        // 解析分页参数
        Map<String, String> params = new HashMap<>();
        var matcher = PAGE_PARAM_PATTERN.matcher(pageParams);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }

        // 检查必填参数 rownum
        if (!params.containsKey("rownum")) {
            return "必须指定 rownum 参数";
        }

        // 验证参数值
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (!value.startsWith(":") && !value.matches("\\d+")) {
                return String.format("%s 的值必须是数字或绑定变量", entry.getKey());
            }
        }

        return null;
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
