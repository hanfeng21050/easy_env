package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class InsertSelectMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String TEMPLATE = "[insertSelect][插入目标表][源表][字段映射][条件语句]";
    private static final String DOC =
            "格式说明：\n" +
                    "1. [插入目标表]: 要插入数据的目标表名\n" +
                    "2. [源表]: 数据来源的表名\n" +
                    "3. [字段映射]: 可选参数，用于指定目标表和源表中不同名的字段映射关系\n" +
                    "   - 如果不填，则只插入目标表和源表中所有同名字段\n" +
                    "   - 格式为：目标字段=源字段，多个映射用逗号分隔\n" +
                    "4. [条件语句]: WHERE子句的完整条件，必须包含绑定变量\n" +
                    "\n" +
                    "示例：\n" +
                    "@JRESMacro(\"[insertSelect][act_fund_account_control_jour][act_fund_account_jour][handle_flag=witness_flag][branch_no=:branch_no and op_entrust_way=:op_entrust_way]\")\n" +
                    "说明：该示例会把 act_fund_account_jour 表中的 witness_flag 字段的值插入到 act_fund_account_control_jour 表的 handle_flag 字段中\n";

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern FIELD_MAPPING_PATTERN = Pattern.compile("^\\w+\\s*=\\s*\\w+$");
    private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":\\w+");

    public InsertSelectMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // 检查参数个数
        if (params.size() < 3) {
            return generateError("错误：参数不足，至少需要提供[宏名称]、[插入目标表]和[源表]", TEMPLATE, DOC);
        }

        // 检查目标表名
        String targetTable = handleException(() -> params.get(1));
        if (!isValidTableName(targetTable)) {
            return generateError("错误：无效的目标表名 '" + targetTable + "'，表名只能包含字母、数字和下划线", TEMPLATE, DOC);
        }

        // 检查源表名
        String sourceTable = handleException(() -> params.get(2));
        if (!isValidTableName(sourceTable)) {
            return generateError("错误：无效的源表名 '" + sourceTable + "'，表名只能包含字母、数字和下划线", TEMPLATE, DOC);
        }

        // 检查字段映射（如果有）
        Map<String, String> fieldMappings = new LinkedHashMap<>();
        if (params.size() > 3 && StringUtils.isNotBlank(params.get(3))) {
            String mappingError = parseFieldMappings(params.get(3), fieldMappings);
            if (mappingError != null) {
                return generateError(mappingError, TEMPLATE, DOC);
            }
        }

        // 检查条件语句（如果有）
        if (params.size() > 4) {
            String whereClause = params.get(4);
            if (StringUtils.isNotBlank(whereClause)) {
                String conditionError = validateWhereClause(whereClause);
                if (conditionError != null) {
                    return generateError(conditionError, TEMPLATE, DOC);
                }
            }
        }

        // 验证生成的完整SQL语句
        String error = validateFullSql(targetTable, sourceTable, fieldMappings,
                params.size() > 4 ? params.get(4) : null);
        if (error != null) {
            return generateError(error, TEMPLATE, DOC);
        }

        return null;
    }

    private boolean isValidTableName(String tableName) {
        return StringUtils.isNotBlank(tableName) && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    private String parseFieldMappings(String mappings, Map<String, String> result) {
        if (StringUtils.isBlank(mappings)) {
            return null;
        }

        String[] pairs = mappings.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (!FIELD_MAPPING_PATTERN.matcher(pair).matches()) {
                return "错误：无效的字段映射格式 '" + pair + "'，应为 'targetField=sourceField'";
            }

            String[] parts = pair.split("=");
            String targetField = parts[0].trim();
            String sourceField = parts[1].trim();

            if (result.containsKey(targetField)) {
                return "错误：目标字段 '" + targetField + "' 重复映射";
            }
            result.put(targetField, sourceField);
        }

        return null;
    }

    private String validateWhereClause(String whereClause) {
        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + whereClause);
            return null;
        } catch (Exception e) {
            return "错误：无效的条件语句 - " + e.getMessage();
        }
    }

    private String validateFullSql(String targetTable, String sourceTable,
                                   Map<String, String> fieldMappings, String whereClause) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(targetTable).append(" (");

        // 添加目标字段
        String targetFields = String.join(",", fieldMappings.keySet());
        sql.append(targetFields).append(") SELECT ");

        // 添加源字段
        String sourceFields = String.join(",", fieldMappings.values());
        sql.append(sourceFields).append(" FROM ").append(sourceTable);

        // 添加WHERE子句
        if (StringUtils.isNotBlank(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }

        try {
            CCJSqlParserUtil.parse(sql.toString());
            return null;
        } catch (Exception e) {
            return "错误：生成的SQL语句无效 - " + e.getMessage();
        }
    }
}
