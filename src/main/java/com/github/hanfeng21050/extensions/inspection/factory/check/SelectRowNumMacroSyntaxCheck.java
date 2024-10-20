package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 21:57
 */
public class SelectRowNumMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String template = "[selectRowNum][sql][rownum=请求行数][查询字段][条件语句][自定义动态条件][分组排序语句]";
    String doc = """
            1、第一个参数必填，为普通SQL语句；第二个参数为请求行数，需要取前几行；第三、第四、第五、第六个参数选填
            2、对于简单SQL第三个参数可以不填，联表查询或子查询等复杂SQL需要填写第三个参数，第三个参数为子查询返回字段
            3、第四个参数为固定的条件语句，即肯定会作为查询条件的完整语句（不包含where关键字）
            4、第五个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中
            5、第六个参数可以填写order by、group by等需要放到SQL结尾的语句，当使用自定义动态条件时，order by、group by等语句一定要写到这里，否则生成的SQL会有问题，用法见例5
            6、例1为简单的SQL查询；例2为联合查询，第三个参数必须为子查询返回的字段；例3为[sql]中不包含任何条件，所有条件都是动态生成的，例4为[sql]中包含某些确定的条件，且需要动态拼接某些条件
            7、若有场景需要用别名方式生成，可使用宏标记“A”，用法见例6
            8、动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字，用法见例7
            例1: @JRESMacro("[selectRowNum][select * from crt_sys_arg][rownum=1000][][user_id = :userId]") 或者   @JRESMacro("[selectRowNum][select * from crt_sys_arg where user_id = :userId][rownum=1000]")
            例2: @JRESMacro("[selectRowNum][select * from (select company_no, company_name from opt_fundreal where client_id = :client_id)][rownum=1000][company_no, company_name]")
            例3: @JRESMacro("[selectRowNum][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][][user_id=:userId, company_no = :companyNo]")
            例4: @JRESMacro("[selectRowNum][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][user_id='123' and company_no = '1'][fund_account = :fund_account]")
            例5: @JRESMacro("[selectRowNum][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][client_id =:client_id and branch_no = '8888'][fund_account = :fund_account][order by init_date]")
            例6: @JRESMacro("<A>[selectRowNum][select * from crt_sys_arg][rownum=1000][][user_id = :userId]")
            例7: @JRESMacro("[selectRowNum][select hs_nvl(risk_level_old, 0),init_date from table][rownum=1000][][][user_id=:userId, /*#OR*/company_no = :companyNo]")
            """;
    public SelectRowNumMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        // [sql]
        String param1 = handleException(() -> params.get(1));
        // [rownum=请求行数]
        String param2 = handleException(() -> params.get(2));
        // [查询字段]
        String param3 = handleException(() -> params.get(3));
        // [条件语句]
        String param4 = handleException(() -> params.get(4));
        // [自定义动态条件]
        String param5 = handleException(() -> params.get(5));
        // [分组排序语句]
        String param6 = handleException(() -> params.get(6));

        if (StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[sql].", template, doc);
        } else {
            try {
                CCJSqlParserUtil.parse(param1);
            } catch (Exception e) {
                return generateError("错误：无法解析[sql].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isBlank(param2) || !param2.toLowerCase().contains("rownum")) {
            return generateError("错误：无法解析[rownum=请求行数].", template, doc);
        }

        if (StringUtils.isNotBlank(param3)) {
            try {
                CCJSqlParserUtil.parse("select " + param3 + " from table");
            } catch (Exception e) {
                return generateError("错误：无法解析[查询字段].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param4)) {
            try {
                CCJSqlParserUtil.parse("select * from table where " + param4);
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);
            }
        }

        if (StringUtils.isNotBlank(param5)) {
//            param5 = param3.replace(",", " and ");
//            try {
//                CCJSqlParserUtil.parse("select * from table where " + param5);
//            } catch (Exception e) {
//                return generateError("错误：无法解析[自定义动态条件].", e.getMessage(), template, doc);
//            }
        }

        if (StringUtils.isNotBlank(param6)) {
            try {
                CCJSqlParserUtil.parse("select * from table " + param6);
            } catch (Exception e) {
                return generateError("错误：无法解析[分组排序语句].", e.getMessage(), template, doc);
            }
        }

        return null;
    }
}
