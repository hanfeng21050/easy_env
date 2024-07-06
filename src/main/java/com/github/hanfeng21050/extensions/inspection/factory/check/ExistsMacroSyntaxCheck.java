package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 11:12
 */
public class ExistsMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String existsTemplate = "[exists]<T>[表名][条件语句][自定义动态条件]";
    String deleteTemplate = "[delete][表名][条件语句][自定义动态条件]";
    String existsDoc = """
            宏标记说明：
            T：打上该标记后，第二个参数会作为完整sql用select count(0)或select exists包装起来，一般用于联表查询，此时第三个参数不可用
            第三个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中
            动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字，用法见例3
            例1: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            例2: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level][client_id =:client_id]")
            例3: @JRESMacro("[exists][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk][risk_level<:risk_level, /*#OR*/client_id =:client_id]")
            """;
    String deleteDoc = """
            1、第一、二个参数为必填，第三个参数[自定义动态条件]选填
            2、第三个参数为动态拼接的条件语句，当变量值不为null时才会被拼接到sql语句中；例2的所有条件都是动态生成的，例3中包含某些确定的条件，且需要动态拼接某些条件
            3、动态条件中可以使用/*#OR*/标记，生成代码时会用or关键字代替and关键字，用法见例4
            例1：@JRESMacro("[delete][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
            例2：@JRESMacro("[delete][prd_test][][user_id = :user_id, create_date > :create_date]")
            例3：@JRESMacro("[delete][prd_test][user_id = :user_id][create_date > :create_date]")
            例4：@JRESMacro("[delete][prd_test][][user_id = :user_id, /*#OR*/create_date > :create_date]")
            """;
    private String macroName;
    public ExistsMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        String template = "";
        String doc = "";
        if (macroName.equals("exists")) {
            template = existsTemplate;
            doc = existsDoc;
        } else if (macroName.equals("delete")) {
            template = deleteTemplate;
            doc = deleteDoc;
        }

        // [表名]
        String param1 = handleException(() -> params.get(1));
        // [条件语句]
        String param2 = handleException(() -> params.get(2));
        // [自定义动态条件]
        String param3 = handleException(() -> params.get(3));

        if ((StringUtils.isNotBlank(param1) && !param1.matches("\\w+")) || StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[表名].", template, doc);

        }

        if (StringUtils.isNotBlank(param2)) {
            try {
                CCJSqlParserUtil.parse("select * from table where " + param2);
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);

            }
        }

        if (StringUtils.isNotBlank(param3)) {
            param3 = param3.replace(",", " and ");
            try {
                CCJSqlParserUtil.parse("select * from table where " + param3);
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);
            }
        }

        return null;
    }
}
