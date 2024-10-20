package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 13:17
 */
public class SelectMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private final String macroName;
    private final String selectTemplate = "模板：[select][sql][查询字段][条件语句][自定义动态条件][分组排序语句]";
    private final String selectListTemplate = "模板：[selectList][sql][查询字段][条件语句][自定义动态条件][分组排序语句]";
    private final String selectsDoc = """
            1、第一个参数[sql]为必填，第二、三、四个参数选填
            2、对于简单SQL第二个参数可以不填，联表查询或子查询等复杂SQL需要填写第二个参数
            3、第三个参数为固定的条件语句，即肯定会作为查询条件的完整语句（不包含where关键字）
            4、第四个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中；例3为[sql]中不包含任何条件，所有条件都是动态生成的，例4为[sql]中包含某些确定的条件，且需要动态拼接某些条件
            5、第五个参数可以填写order by、group by等需要放到SQL结尾的语句，当使用自定义动态条件时，order by、group by等语句一定要写到这里，否则生成的SQL会有问题，用法见例5
            6、若有场景需要用别名方式生成，可使用宏标记“A”，用法见例6
            例1: @JRESMacro("[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = 1]")
            例2: @JRESMacro("[select][select * from (select client_id, exchange_type, current_balance from opt_fundreal where client_id = :client_id)][client_id, exchange_type, current_balance]")
            例3: @JRESMacro("[select][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, branch_no = :branch_no]")
            例4: @JRESMacro("[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account]")
            例5: @JRESMacro("[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account][order by init_date]")
            例6: @JRESMacro("<A>[select][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = 1]")
            例7: @JRESMacro("[select][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, /*#OR*/branch_no = :branch_no]")
            """;
    private final String selectListDoc = """
            1、第一个参数[sql]为必填，第二、三、四个参数选填
            2、对于简单SQL第二个参数可以不填，联表查询或子查询等复杂SQL需要填写第二个参数
            3、第三个参数为固定的条件语句，即肯定会作为查询条件的完整语句（不包含where关键字）
            4、第四个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中；例3为[sql]中不包含任何条件，所有条件都是动态生成的，例4为[sql]中包含某些确定的条件，且需要动态拼接某些条件
            5、第五个参数可以填写order by、group by等需要放到SQL结尾的语句，当使用自定义动态条件时，order by、group by等语句一定要写到这里，否则生成的SQL会有问题，用法见例5
            6、若有场景需要用别名方式生成，可使用宏标记“A”，用法见例6
            7、动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字，用法见例7
            例1: @JRESMacro("[selectList][select risk_level_old,init_date,corp_risk_level from elg_client_risk_calm][][client_id = :client_id and branch_no > :branch_no]")
            例2: @JRESMacro("[selectList][select * from (select client_id, exchange_type, current_balance from opt_fundreal where client_id = :client_id)][client_id, exchange_type, current_balance]")
            例3: @JRESMacro("[selectList][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, branch_no = :branch_no]")
            例4: @JRESMacro("[selectList][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account]")
            例5: @JRESMacro("[selectList][select hs_nvl(risk_level_old, 0),init_date from table][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account][order by init_date]")
            例6: @JRESMacro("<A>[selectList][select risk_level_old,init_date,corp_risk_level from elg_client_risk_calm][][client_id = :client_id and branch_no > :branch_no]")
            例7: @JRESMacro("[selectList][select hs_nvl(risk_level_old, 0),init_date from table][][][client_id =:client_id, /*#OR*/branch_no = :branch_no]")
            """;

    public SelectMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // [sql]
        String param1 = handleException(() -> params.get(1));
        // [查询字段]
        String param2 = handleException(() -> params.get(2));
        // [条件语句]
        String param3 = handleException(() -> params.get(3));
        // [自定义动态条件]
        String param4 = handleException(() -> params.get(4));
        // [分组排序语句]
        String param5 = handleException(() -> params.get(5));


        String template = macroName.equals("select") ? selectTemplate : selectListTemplate;
        String doc = macroName.equals("doc") ? selectsDoc : selectListDoc;

        if (StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[sql].", template, doc);
        } else {
            try {
                CCJSqlParserUtil.parse(param1);
            } catch (Exception e) {
                return generateError("错误：无法解析[sql].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param2)) {
            try {
                CCJSqlParserUtil.parse("select " + param2 + " from table");
            } catch (Exception e) {
                return generateError("错误：无法解析[查询字段].", e.getMessage(), template, doc);
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
//            param4 = param3.replace(",", " and ");
//            try {
//                CCJSqlParserUtil.parse("select * from table where " + param4);
//            } catch (Exception e) {
//                return generateError("错误：无法解析[自定义动态条件].", e.getMessage(), template, doc);
//            }
        }

        if (StringUtils.isNotBlank(param5)) {
            try {
                CCJSqlParserUtil.parse("select * from table " + param5);
            } catch (Exception e) {
                return generateError("错误：无法解析[分组排序语句].", e.getMessage(), template, doc);
            }
        }

        return null;
    }
}
