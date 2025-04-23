package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 11:12
 */
public class ExistsMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String EXISTS_TEMPLATE = "[exists]<T>[表名][SQL条件语句][自定义动态条件]";
    private static final String DELETE_TEMPLATE = "[delete][表名][SQL条件语句][自定义动态条件]";

    private static final String EXISTS_DOC = """
            宏标记说明：
            T：打上该标记后，第二个参数会作为完整sql用select count(0)或select exists包装起来，一般用于联表查询，此时第三个参数不可用
            第三个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中
            动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字，用法见例3
            例1: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            例2: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level][client_id =:client_id]")
            例3: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk][risk_level<:risk_level, /*#OR*/client_id =:client_id]")
            """;

    private static final String DELETE_DOC = """
            1、第一、二个参数为必填，第三个参数[自定义动态条件]选填
            2、第三个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中
            3、动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字
            例1：@JRESMacro("[delete][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            例2：@JRESMacro("[delete][prd_test][][user_id = :user_id, create_date > :create_date]")
            例3：@JRESMacro("[delete][prd_test][user_id = :user_id][create_date > :create_date]")
            例4：@JRESMacro("[delete][prd_test][][user_id = :user_id, /*#OR*/create_date > :create_date]")
            """;

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":\\w+");

    private final String macroName;

    public ExistsMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        String template = macroName.equals("exists") ? EXISTS_TEMPLATE : DELETE_TEMPLATE;
        String doc = macroName.equals("exists") ? EXISTS_DOC : DELETE_DOC;

        // 检查参数个数
        if (params.size() < 3) {
            return generateError("错误：参数不足，至少需要提供[宏名称]、[表名]和[SQL条件语句]", template, doc);
        }

        // 检查表名
        String tableName = handleException(() -> params.get(1));
        if (!isValidTableName(tableName)) {
            return generateError("错误：无效的表名 '" + tableName + "'，表名只能包含字母、数字和下划线", template, doc);
        }

        // 检查是否有T标记
        boolean hasTFlag = params.get(0).contains("T");

        // 检查SQL条件语句
        String sqlCondition = handleException(() -> params.get(2));
        if (!isValidSqlCondition(sqlCondition, hasTFlag)) {
            return generateError("错误：无效的SQL条件语句 '" + sqlCondition + "'", template, doc);
        }

        // 如果有T标记，不允许有第三个参数
        if (hasTFlag && params.size() > 3 && StringUtils.isNotBlank(params.get(3))) {
            return generateError("错误：使用T标记时不能包含第三个参数（动态条件）", template, doc);
        }

        // 检查动态条件（如果有）
        if (params.size() > 3) {
            String dynamicCondition = handleException(() -> params.get(3));
            if (!isValidDynamicCondition(dynamicCondition)) {
                return generateError("错误：无效的动态条件 '" + dynamicCondition + "'", template, doc);
            }
        }

        return null;
    }

    private boolean isValidTableName(String tableName) {
        return StringUtils.isNotBlank(tableName) && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    private boolean isValidSqlCondition(String condition, boolean isTotalSql) {
        if (StringUtils.isBlank(condition)) {
            return false;
        }

        try {
            if (isTotalSql) {
                // T标记时，condition是完整的SQL
                CCJSqlParserUtil.parse(condition);
            } else {
                if (StringUtils.isNotBlank(condition)) {
                    // 非T标记时，condition是WHERE子句
                    CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + condition);
                }
            }

            // 检查是否包含绑定变量
            return BIND_VARIABLE_PATTERN.matcher(condition).find();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidDynamicCondition(String condition) {
        if (StringUtils.isBlank(condition)) {
            return true; // 动态条件可以为空
        }

        // 替换OR标记为AND，以便进行SQL语法检查
        String normalizedCondition = condition.replace("/*#OR*/", "AND")
                .replace(",", " AND ");

        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + normalizedCondition);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
