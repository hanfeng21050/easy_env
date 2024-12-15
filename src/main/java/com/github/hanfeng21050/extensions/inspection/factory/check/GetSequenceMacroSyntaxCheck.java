package com.github.hanfeng21050.extensions.inspection.factory.check;

import com.github.hanfeng21050.extensions.inspection.factory.MacroSyntaxCheck;
import com.github.hanfeng21050.extensions.inspection.factory.SyntaxChecker;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

public class GetSequenceMacroSyntaxCheck extends MacroSyntaxCheck implements SyntaxChecker {
    private static final String TEMPLATE = "[getSequence][表名][序列名]";
    private static final String DOC = """
            宏定义说明:
            1. [表名] - 必填参数
               - 数据库表名
               - 只能包含字母、数字和下划线
            
            2. [序列名] - 必填参数
               - Oracle序列名称
               - 只能包含字母、数字和下划线
            
            注意事项:
            - 方法参数类型必须为List集合
            - 返回值类型为Long
            
            示例:
            @JRESMacro("[getSequence][sps_sequence][entrustseq]")
            Long getFastCounter();
            """;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w*$");

    public GetSequenceMacroSyntaxCheck(String value) {
        super(value);
    }

    @Override
    public String performSyntaxCheck(List<String> params) {
        if (params.size() < 3) {
            return generateError("错误：缺少必要参数", TEMPLATE, DOC);
        }

        // 检查表名
        String tableName = handleException(() -> params.get(1));
        if (StringUtils.isBlank(tableName)) {
            return generateError("错误：[表名]不能为空", TEMPLATE, DOC);
        }
        if (!NAME_PATTERN.matcher(tableName).matches()) {
            return generateError("错误：[表名]格式无效，只能包含字母、数字和下划线，且必须以字母开头", TEMPLATE, DOC);
        }

        // 检查序列名
        String sequenceName = handleException(() -> params.get(2));
        if (StringUtils.isBlank(sequenceName)) {
            return generateError("错误：[序列名]不能为空", TEMPLATE, DOC);
        }
        if (!NAME_PATTERN.matcher(sequenceName).matches()) {
            return generateError("错误：[序列名]格式无效，只能包含字母、数字和下划线，且必须以字母开头", TEMPLATE, DOC);
        }

        return null;
    }
}
