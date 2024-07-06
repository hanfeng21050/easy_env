package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author hanfeng32305
 * @Date 2024/6/30 20:19
 */
public class InsertMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    String insertTemplate = "[insert][表名][参数列表]";
    String batchInsertTemplate = "[batchInsert][表名][参数列表]";
    String batchDeleteTemplate = "[batchDelete][表名][参数列表]";
    String getSerialNoTemplate = "[batchDelete][表名][参数列表]";
    String getPrefetchSeqTemplate = "[batchDelete][表名][参数列表]";
    String insertDoc = """
            @JRESMacro("[insert][elg_client_risk_calm][branch_no=:branch_no,client_id]")
            """;
    String batchInsertDoc = """
            @JRESMacro("[batchInsert][elg_client_risk_calm][branch_no=:branch_no,client_id]")
            批量插入，方法参数类型必须为List集合！
            """;
    String batchDeleteDoc = """
            @JRESMacro("[batchDelete][elg_client_risk_calm][init_date=20190301 and risk_level>:old_risk and risk_level<:risk_level and client_id=:client_id]")
             批量删除，方法参数类型必须为List集合！
            """;
    String getSerialNoDoc = """
            @JRESMacro("[insert][elg_client_risk_calm][branch_no=:branch_no,client_id]")
            """;
    String getPrefetchSeqDoc = """
            @JRESMacro("[getPrefetchSeq][ses_prefetch_seq][client_id = :client_id]")
            """;
    private String macroName;
    public InsertMacroSyntaxCheck(String macroName, String value) {
        super(value);
        this.macroName = macroName;
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        String template = "";
        String doc = "";
        if (this.macroName.equals("insert")) {
            template = insertTemplate;
            doc = insertDoc;
        } else if (this.macroName.equals("batchInsert")) {
            template = batchInsertTemplate;
            doc = batchInsertDoc;
        } else if (this.macroName.equals("batchDelete")) {
            template = batchDeleteTemplate;
            doc = batchDeleteDoc;
        } else if (this.macroName.equals("getSerialNo")) {
            template = getSerialNoTemplate;
            doc = getSerialNoDoc;
        } else if (this.macroName.equals("getPrefetchSeq")) {
            template = getPrefetchSeqTemplate;
            doc = getPrefetchSeqDoc;
        }

        // [表名]
        String param1 = handleException(() -> params.get(1));
        // [参数列表]
        String param2 = handleException(() -> params.get(2));

        if ((StringUtils.isNotBlank(param1) && !param1.matches("\\w+")) || StringUtils.isBlank(param1)) {
            return generateError("错误：无法解析[表名].", template, doc);
        }

        if (StringUtils.isBlank(param2)) {
            return generateError("错误：无法解析[参数列表].", template, doc);
        } else {
            Map<String, String> stringStringMap = convertKVString(param2);
            String[] keys = stringStringMap.keySet().toArray(new String[0]);
            Arrays.sort(keys);

            String[] values = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                values[i] = stringStringMap.get(keys[i]);
            }

            String keyStr = String.join(",", keys);
            String valueStr = String.join("','", values);

            try {
                CCJSqlParserUtil.parse("insert into table (" + keyStr + ") values('" + valueStr + "')");
            } catch (Exception e) {
                return generateError("错误：无法解析[条件语句].", e.getMessage(), template, doc);
            }
        }


        return null;
    }
}
