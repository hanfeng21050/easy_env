package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 20:19
 */
public class InsertMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String INSERT_TEMPLATE = "[insert][表名][字段列表]";
    private static final String BATCH_INSERT_TEMPLATE = "[batchInsert][表名][字段列表]";
    private static final String BATCH_DELETE_TEMPLATE = "[batchDelete][表名][条件语句]";
    private static final String GET_SERIAL_NO_TEMPLATE = "[getSerialNo][表名][字段列表]";
    private static final String GET_PREFETCH_SEQ_TEMPLATE = "[getPrefetchSeq][表名][条件语句]";

    private static final String INSERT_DOC = """
            插入单条记录
            格式：@JRESMacro("[insert][表名][字段1=:参数1,字段2,字段3=:参数3]")
            说明：
            1. 字段列表中的字段可以是以下两种形式：
               - 字段名=:参数名 （显式指定参数）
               - 字段名 （隐式使用同名参数）
            2. 所有字段都会从传入的对象中获取对应的值
            例子：
            @JRESMacro("[insert][elg_client_risk_calm][branch_no=:branch_no,client_id]")
            """;

    private static final String BATCH_INSERT_DOC = """
            批量插入记录
            格式：@JRESMacro("[batchInsert][表名][字段1=:参数1,字段2,字段3=:参数3]")
            说明：
            1. 方法参数类型必须为List集合
            2. 字段格式与单条插入相同
            例子：
            @JRESMacro("[batchInsert][elg_client_risk_calm][branch_no=:branch_no,client_id]")
            """;

    private static final String BATCH_DELETE_DOC = """
            批量删除记录
            格式：@JRESMacro("[batchDelete][表名][条件语句]")
            说明：
            1. 方法参数类型必须为List集合
            2. 条件语句必须包含绑定变量
            例子：
            @JRESMacro("[batchDelete][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and client_id=:client_id]")
            """;

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^\\w+$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^\\w+(?:=:\\w+)?$");
    private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":\\w+");

    private final String macroName;

    public InsertMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        String template = getTemplate();
        String doc = getDoc();

        // 检查参数个数
        if (params.size() < 3) {
            return generateError("错误：参数不足，至少需要提供[宏名称]、[表名]和[字段列表]", template, doc);
        }

        // 检查表名
        String tableName = handleException(() -> params.get(1));
        if (!isValidTableName(tableName)) {
            return generateError("错误：无效的表名 '" + tableName + "'，表名只能包含字母、数字和下划线", template, doc);
        }

        // 检查字段列表或条件语句
        String fieldsOrCondition = handleException(() -> params.get(2));
        if (StringUtils.isBlank(fieldsOrCondition)) {
            return generateError("错误：[字段列表]或[条件语句]不能为空", template, doc);
        }

        // 根据宏类型执行不同的检查
        if (macroName.equals("insert") || macroName.equals("batchInsert") || macroName.equals("getSerialNo")) {
            return checkInsertFields(fieldsOrCondition, template, doc);
        } else if (macroName.equals("batchDelete") || macroName.equals("getPrefetchSeq")) {
            return checkCondition(fieldsOrCondition, template, doc);
        }

        return null;
    }

    private String getTemplate() {
        return switch (macroName) {
            case "insert" -> INSERT_TEMPLATE;
            case "batchInsert" -> BATCH_INSERT_TEMPLATE;
            case "batchDelete" -> BATCH_DELETE_TEMPLATE;
            case "getSerialNo" -> GET_SERIAL_NO_TEMPLATE;
            case "getPrefetchSeq" -> GET_PREFETCH_SEQ_TEMPLATE;
            default -> INSERT_TEMPLATE;
        };
    }

    private String getDoc() {
        return switch (macroName) {
            case "insert" -> INSERT_DOC;
            case "batchInsert" -> BATCH_INSERT_DOC;
            case "batchDelete" -> BATCH_DELETE_DOC;
            default -> INSERT_DOC;
        };
    }

    private boolean isValidTableName(String tableName) {
        return StringUtils.isNotBlank(tableName) && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }

    private String checkInsertFields(String fields, String template, String doc) {
        // 解析字段列表
        String[] fieldList = fields.split(",");
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (String field : fieldList) {
            field = field.trim();
            if (!FIELD_PATTERN.matcher(field).matches()) {
                return generateError("错误：无效的字段格式 '" + field + "'，应为 'field' 或 'field=:param'", template, doc);
            }

            String[] parts = field.split("=", 2);
            String columnName = parts[0].trim();
            String value = parts.length > 1 ? parts[1].trim() : ":" + columnName;

            columns.add(columnName);
            values.add(value);
        }

        // 检查是否至少有一个字段
        if (columns.isEmpty()) {
            return generateError("错误：字段列表不能为空", template, doc);
        }

        // 验证生成的SQL是否有效
        try {
            String sql = "INSERT INTO table (" + String.join(",", columns) + ") VALUES (" + String.join(",", values) + ")";
            CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            return generateError("错误：生成的SQL语句无效", e.getMessage(), template, doc);
        }

        return null;
    }

    private String checkCondition(String condition, String template, String doc) {
        try {
            CCJSqlParserUtil.parse("SELECT * FROM dual WHERE " + condition);
        } catch (Exception e) {
            return generateError("错误：无效的条件语句", e.getMessage(), template, doc);
        }

        return null;
    }
}
